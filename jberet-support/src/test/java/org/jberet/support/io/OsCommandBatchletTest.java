/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.operations.JobOperatorImpl;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link OsCommandBatchlet}.
 *
 * @since 1.3.0.Beta5
 */
public class OsCommandBatchletTest {
    public static final String jobName = "org.jberet.support.io.OsCommandBatchletTest";
    public static final JobOperator jobOperator = new JobOperatorImpl();

    /**
     * Runs job {@value #jobName}, where {@link OsCommandBatchlet} executes
     * simple OS commands.
     *
     * @throws Exception upon errors
     */
    @Test
    public void simpleCommands() throws Exception {
        // run echo command, and should complete successfully with process exit code 0.
        final Properties jobParams = new Properties();
        jobParams.setProperty("commandLine", "echo This is echo from osCommandBatchlet");
        runCommand(jobParams, BatchStatus.COMPLETED, String.valueOf(0));

        // run echo command, passing the command as comma-separated list,
        // and setting custom working directory and timeout.
        // The command should complete successfully with process exit code 0.
        jobParams.clear();
        jobParams.setProperty("commandArray", "echo, abc, xyz, 123");
        jobParams.setProperty("workingDir", System.getProperty("java.io.tmpdir"));
        jobParams.setProperty("timeoutSeconds", String.valueOf(600));
        runCommand(jobParams, BatchStatus.COMPLETED, String.valueOf(0));

        // run cd command, setting the process exit code for successful completion to 999999.
        // The job execution should fail, since the process exit code 0 does not match 999999.
        jobParams.clear();
        jobParams.setProperty("commandLine", "cd ..");
        jobParams.setProperty("commandOkExitValues", String.valueOf(999999));
        runCommand(jobParams, BatchStatus.FAILED, String.valueOf(0));
    }

    /**
     * Runs {@code top} command, which continuously displays OS process info.
     * By setting a timeout, the {@code top} process should be aborted after timeout,
     * and so the job execution should fail, and the batch exit status is set to
     * the process exit code (143, interrupted).
     *
     * @throws Exception upon errors
     */
    @Test
    public void timeout() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("commandLine", "top");
        jobParams.setProperty("timeoutSeconds", String.valueOf(5));
        runCommand(jobParams, BatchStatus.FAILED, String.valueOf(143));
    }

    /**
     * Runs {@code top} command, which continuously displays OS process info.
     * The job execution is then stopped, which means the {@code top} command
     * should also be stopped.  The batch exit status is set to the process
     * exit code (143).
     *
     * @throws Exception upon errors
     */
    @Test
    public void stop() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("commandLine", "top");
        final long jobExecutionId = jobOperator.start(jobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);

        Thread.sleep(3000);
        jobOperator.stop(jobExecutionId);
        Thread.sleep(2000);
        checkJobExecution(jobExecution, BatchStatus.STOPPED, String.valueOf(143));
    }

    protected void runCommand(final Properties jobParams,
                              final BatchStatus expectedBatchStatus,
                              final String expectedExitStatus) throws Exception {
        final long jobExecutionId = jobOperator.start(jobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);
        checkJobExecution(jobExecution, expectedBatchStatus, expectedExitStatus);
    }

    protected void checkJobExecution(final JobExecutionImpl jobExecution,
                                     final BatchStatus expectedBatchStatus,
                                     final String expectedExitStatus) {
        assertEquals(expectedBatchStatus, jobExecution.getBatchStatus());
        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        assertEquals(1, stepExecutions.size());
        final StepExecution stepExecution = stepExecutions.get(0);
        assertEquals(expectedExitStatus, stepExecution.getExitStatus());
    }
}
