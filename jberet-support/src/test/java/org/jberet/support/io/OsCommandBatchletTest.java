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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.operations.JobOperatorImpl;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.Test;

/**
 * Tests for {@link OsCommandBatchlet}.
 *
 * @since 1.3.0.Beta5
 */
public class OsCommandBatchletTest {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
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
        String cmd;
        if (IS_WINDOWS) {
            cmd = "cmd.exe /C echo This is echo from osCommandBatchlet";
        } else {
            cmd = "echo This is echo from osCommandBatchlet";
        }
        jobParams.setProperty("commandLine", cmd);
        runCommand(jobParams, BatchStatus.COMPLETED, true);

        // run echo command, passing the command as comma-separated list,
        // and setting custom working directory and timeout.
        // The command should complete successfully with process exit code 0.
        jobParams.clear();
        if (IS_WINDOWS) {
            cmd = "cmd.exe, /C, echo, abc, xyz, 123";
        } else {
            cmd = "echo, abc, xyz, 123";
        }
        jobParams.setProperty("commandArray", cmd);
        jobParams.setProperty("workingDir", System.getProperty("java.io.tmpdir"));
        jobParams.setProperty("timeoutSeconds", String.valueOf(600));
        runCommand(jobParams, BatchStatus.COMPLETED, true);

        // run cd command, setting the process exit code for successful completion to 999999.
        // The job execution should fail, since the process exit code 0 does not match 999999.
        jobParams.clear();
        if (IS_WINDOWS) {
            cmd = "cmd.exe /C cd ..";
        } else {
            cmd = "cd ..";
        }
        jobParams.setProperty("commandLine", cmd);
        jobParams.setProperty("commandOkExitValues", String.valueOf(999999));
        runCommand(jobParams, BatchStatus.FAILED, true);
    }

    /**
     * This test uses a custom stream handler that does nothing.
     * There should be no output displayed from running the command subprocess.
     *
     * @throws Exception upon errors
     */
    @Test
    public void streamHandler() throws Exception {
        // run echo command, and should complete successfully with process exit code 0.
        final Properties jobParams = new Properties();
        final String cmd;
        if (IS_WINDOWS) {
            cmd = "cmd.exe /C echo This is echo from osCommandBatchlet";
        } else {
            cmd = "echo This is echo from osCommandBatchlet";
        }
        jobParams.setProperty("commandLine", cmd);
        jobParams.setProperty("streamHandler", "org.jberet.support.io.OsCommandBatchletTest$NoopStreamHandler");
        runCommand(jobParams, BatchStatus.COMPLETED, true);
        assertFalse("Expected the stream handler to be stopped.", NoopStreamHandler.started.get());
    }

    /**
     * Runs {@code sleep} ({@code ping} on Windows) command, which will block for at least 10 seconds. The batch exit
     * status is set to the process exit code.
     *
     * @throws Exception upon errors
     */
    @Test
    public void timeout() throws Exception {
        final Properties jobParams = new Properties();
        if (IS_WINDOWS) {
            jobParams.setProperty("commandLine", "cmd.exe /C ping -n 10 127.0.0.1");
        } else {
            jobParams.setProperty("commandLine", "sleep 10");
        }
        jobParams.setProperty("timeoutSeconds", String.valueOf(5));
        runCommand(jobParams, BatchStatus.FAILED, false);
    }

    /**
     * Runs {@code sleep} ({@code ping} on Windows) command, which will block for at least 10 seconds. The batch exit
     * status is set to the process exit code.
     *
     * @throws Exception upon errors
     */
    @Test
    public void stop() throws Exception {
        final Properties jobParams = new Properties();
        if (IS_WINDOWS) {
            jobParams.setProperty("commandLine", "cmd.exe /C ping -n 10 127.0.0.1");
        } else {
            jobParams.setProperty("commandLine", "sleep 10");
        }
        final long jobExecutionId = jobOperator.start(jobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);

        Thread.sleep(3000);
        assertEquals(String.format("Job is not started. Exit status %s", jobExecution.getExitStatus()),
                BatchStatus.STARTED, jobExecution.getBatchStatus());
        jobOperator.stop(jobExecutionId);
        Thread.sleep(2000);
        checkJobExecution(jobExecution, BatchStatus.STOPPED, false);
    }

    protected void runCommand(final Properties jobParams,
                              final BatchStatus expectedBatchStatus,
                              final boolean expectSuccess) throws Exception {
        final long jobExecutionId = jobOperator.start(jobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);
        checkJobExecution(jobExecution, expectedBatchStatus, expectSuccess);
    }

    /**
     * Checks the job execution.
     * <p>
     * Assumes there is only one step and that the step exit status is either {@code 0} or not {@code 0}. An exit code
     * of {@code 0} is assumed to be successful any other value is expected to be a failure. The {@code expectSuccess}
     * should be {@code true} if a {@code 0} exit code is expected.
     * </p>
     *
     * @param jobExecution        if the job execution used to get the steps
     * @param expectedBatchStatus the expected batch status
     * @param expectSuccess       {@code true} if the exist status (the processes exit code) should be {@code 0}, otherwise
     *                            {@code false}
     */
    protected void checkJobExecution(final JobExecutionImpl jobExecution,
                                     final BatchStatus expectedBatchStatus,
                                     final boolean expectSuccess) {
        assertEquals(expectedBatchStatus, jobExecution.getBatchStatus());
        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        assertEquals(1, stepExecutions.size());
        final StepExecution stepExecution = stepExecutions.get(0);
        if (expectSuccess) {
            assertEquals("0", stepExecution.getExitStatus());
        } else {
            assertNotEquals("0", stepExecution.getExitStatus());
        }
    }

    public static final class NoopStreamHandler implements StreamHandler {
        static final AtomicBoolean started = new AtomicBoolean();

        @Override
        public void setProcessInputStream(final OutputStream os) throws IOException {

        }

        @Override
        public void setProcessErrorStream(final InputStream is) throws IOException {

        }

        @Override
        public void setProcessOutputStream(final InputStream is) throws IOException {

        }

        @Override
        public void start() {
            if (!started.compareAndSet(false, true)) {
                throw new RuntimeException("There was an attempt to restart a StreamHandler before it's been stopped");
            }
        }

        @Override
        public void stop() {
            started.set(false);
        }
    }

}
