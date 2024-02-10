/*
 * Copyright (c) 2022 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.StepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.jupiter.api.Test;

public class InjectBatchletTest {
    Logger logger = Logger.getLogger(getClass().getName());
    static final String jobName = "org.jberet.se.test.injectBatchlet";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Test
    public void injectBatchletTest1() throws Exception {
        long jobExecutionId = jobOperator.start(jobName, null);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final String exitStatus = jobExecution.getExitStatus();
        logger.info("Job exit status: " + exitStatus);
        String expected = String.format("%s:%s:%s:", jobExecutionId, jobExecutionId, jobExecutionId);
        assertEquals("Wrong job exit status", expected, exitStatus);

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        final long stepExecutionId = stepExecution.getStepExecutionId();
        final String stepExecutionExitStatus = stepExecution.getExitStatus();
        logger.info("Step exit status: " + stepExecutionExitStatus);
        String expectedStepExitStatus = String.format("%s:%s:%s:", stepExecutionId, stepExecutionId, stepExecutionId);
        assertEquals("Wrong step exit status", expectedStepExitStatus, stepExecutionExitStatus);
    }

}
