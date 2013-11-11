/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.se.test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SleepBatchletTest {
    private final JobOperator operator = BatchRuntime.getJobOperator();
    private static final String jobName = "org.jberet.se.test.sleepBatchlet.xml";

    @Test
    @Ignore("takes too long")
    public void sleepComplete() throws Exception {
        final int sleepMinutes = 6;
        final Properties params = new Properties();
        params.setProperty("sleep.minutes", String.valueOf(sleepMinutes));
        final long jobExecutionId = operator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(sleepMinutes * 2, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assert.assertEquals("Slept", stepExecution.getExitStatus());
    }

    @Test
    public void sleepStop() throws Exception {
        final int sleepMinutes = 6;
        final Properties params = new Properties();
        params.setProperty("sleep.minutes", String.valueOf(sleepMinutes));
        final long jobExecutionId = operator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        operator.stop(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.STOPPED.name(), jobExecution.getExitStatus());

        StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.STOPPED, stepExecution.getBatchStatus());
        Assert.assertEquals("Interrupted", stepExecution.getExitStatus());
    }
}
