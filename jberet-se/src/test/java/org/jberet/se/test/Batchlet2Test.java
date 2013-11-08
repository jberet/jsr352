/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public class Batchlet2Test {
    static final String jobName = "org.jberet.se.test.batchlet2";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Test
    public void testStopWithRestartPoint() throws Exception {
        final Properties params = Batchlet1Test.createParams(Batchlet1.ACTION, Batchlet1.ACTION_STOP);
        System.out.printf("Start with params %s%n", params);
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT, TimeUnit.SECONDS);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_STOP, jobExecution.getExitStatus());

        Assert.assertEquals(5, jobExecution.getStepExecutions().size());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(1).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(2).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(3).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(4).getBatchStatus());
    }
}
