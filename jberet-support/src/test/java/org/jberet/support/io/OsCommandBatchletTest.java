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

public class OsCommandBatchletTest {
    public static final String jobName = "org.jberet.support.io.OsCommandBatchletTest";
    public static final JobOperator jobOperator = new JobOperatorImpl();

    @Test
    public void simpleCommands() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("commandLine", "echo This is echo from osCommandBatchlet");
        runCommand(jobParams, BatchStatus.COMPLETED, String.valueOf(0));

        jobParams.clear();
        jobParams.setProperty("commandArray", "echo, abc, xyz, 123");
        jobParams.setProperty("workingDir", System.getProperty("java.io.tmpdir"));
        jobParams.setProperty("timeoutSeconds", String.valueOf(600));
        runCommand(jobParams, BatchStatus.COMPLETED, String.valueOf(0));

        jobParams.clear();
        jobParams.setProperty("commandLine", "cd");
        jobParams.setProperty("commandOkExitValues", String.valueOf(999999));
        runCommand(jobParams, BatchStatus.FAILED, String.valueOf(0));
    }

    @Test
    public void timeout() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("commandLine", "top");
        jobParams.setProperty("timeoutSeconds", String.valueOf(5));
        runCommand(jobParams, BatchStatus.FAILED, String.valueOf(143));
    }

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
