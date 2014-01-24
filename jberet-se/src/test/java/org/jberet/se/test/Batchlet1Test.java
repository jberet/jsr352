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
import org.jberet.runtime.StepExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public class Batchlet1Test {
    static final String jobName = "org.jberet.se.test.batchlet1";
    static final String jobName2 = "org.jberet.se.test.batchlet2";
    static final String jobName3 = "org.jberet.se.test.batchlet3";
    static final String jobName4 = "org.jberet.se.test.batchlet4";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final int waitTimeoutMinutes = 0;

    @Test
    public void testBatchlet1() throws Exception {
        long jobExecutionId;
        jobExecutionId = startJobMatchEnd();
        jobExecutionId = startJobMatchOther();
        jobExecutionId = startJobMatchFail();
        jobExecutionId = restartJobMatchStop(jobExecutionId);
    }

    @Test
    public void testStopWithRestartPoint() throws Exception {
        final Properties params = Batchlet1Test.createParams(Batchlet1.ACTION, Batchlet1.ACTION_STOP);
        System.out.printf("Start with params %s%n", params);
        final long jobExecutionId = jobOperator.start(jobName2, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
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

    @Test
    public void testStepFail3Times() throws Exception {
        final JobExecutionImpl[] jobExecutions = new JobExecutionImpl[3];
        final Properties params = Batchlet1Test.createParams(Batchlet1.ACTION, Batchlet1.ACTION_EXCEPTION);
        {
            System.out.printf("Start with params %s%n", params);
            long jobExecutionId = jobOperator.start(jobName3, params);
            JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
            jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
            jobExecutions[0] = jobExecution;

            System.out.printf("Restart with params %s%n", params);
            jobExecutionId = jobOperator.restart(jobExecutionId, params);
            jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
            jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
            jobExecutions[1] = jobExecution;

            System.out.printf("Restart with params %s%n", params);
            jobExecutionId = jobOperator.restart(jobExecutionId, params);
            jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
            jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
            jobExecutions[2] = jobExecution;
        }
        for (final JobExecutionImpl e : jobExecutions) {
            System.out.printf("JobExecution id: %s%n", e.getExecutionId());
            Assert.assertEquals(BatchStatus.FAILED, e.getBatchStatus());
            Assert.assertEquals(BatchStatus.FAILED.name(), e.getExitStatus());

            Assert.assertEquals(1, e.getStepExecutions().size());
            Assert.assertEquals(BatchStatus.FAILED, e.getStepExecutions().get(0).getBatchStatus());
            Assert.assertEquals(Batchlet1.ACTION_EXCEPTION, e.getStepExecutions().get(0).getExitStatus());
        }
    }

    /**
     * stepFailWithLongException will throw an exception with very long message, and the exception should be truncated
     * and stored in job repository STEP_EXECUTION table without causing database error.
     *
     * @throws Exception
     */
    @Test
    public void testStepFailWithLongException() throws Exception {
        final Properties params = Batchlet1Test.createParams(Batchlet1.ACTION, Batchlet1.ACTION_LONG_EXCEPTION);
        System.out.printf("Start with params %s%n", params);
        final long jobExecutionId = jobOperator.start(jobName4, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        final StepExecutionImpl stepExecution = (StepExecutionImpl) jobExecution.getStepExecutions().get(0);
        Assert.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_LONG_EXCEPTION, stepExecution.getExitStatus());

        final Exception exception = stepExecution.getException();
        Assert.assertNotNull(exception);
        final String message = exception.getMessage();
        //System.out.printf("Step exception message: %s%n", message.substring(0, Math.min(message.length(), 1000)));
        Assert.assertEquals(true, message.startsWith(Batchlet1.ACTION_LONG_EXCEPTION));
    }

    static Properties createParams(final String key, final String val) {
        final Properties params = new Properties();
        if (key != null) {
            params.setProperty(key, val);
        }
        return params;
    }

    private long startJobMatchOther() throws Exception {
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_OTHER);
        //start the job and complete step1 and step2, not matching any transition element in step2
        System.out.printf("Start with params %s%n", params);
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        Assert.assertEquals(2, jobExecution.getStepExecutions().size());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getStepExecutions().get(0).getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(1).getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_OTHER, jobExecution.getStepExecutions().get(1).getExitStatus());
        return jobExecutionId;
    }

    private long startJobMatchEnd() throws Exception {
        //start the job and complete step1 and step2, matching <end> element in step2
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_END);
        System.out.printf("Start with params %s%n", params);
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_END, jobExecution.getExitStatus());

        Assert.assertEquals(2, jobExecution.getStepExecutions().size());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getStepExecutions().get(0).getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(1).getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_END, jobExecution.getStepExecutions().get(1).getExitStatus());
        return jobExecutionId;
    }

    private long startJobMatchFail() throws Exception {
        //start the job and fail at the end of step2, matching <fail> element in step2
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_FAIL);
        System.out.printf("Start with params %s%n", params);
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_FAIL, jobExecution.getExitStatus());  //set by <fail> element

        Assert.assertEquals(2, jobExecution.getStepExecutions().size());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getStepExecutions().get(0).getExitStatus());

        // <fail> element does not affect the already-completed step batchlet execution.
        // Although the job FAILED, but step2 still COMPLETED
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(1).getBatchStatus());
        // step2 exit status from batchlet1.process method return value, not from <fail exit-status> element
        Assert.assertEquals(Batchlet1.ACTION_FAIL, jobExecution.getStepExecutions().get(1).getExitStatus());
        return jobExecutionId;
    }

    private long restartJobMatchStop(final long previousJobExecutionId) throws Exception {
        //restart the job and stop at the end of step2, matching <stop> element in step2.
        //next time this job execution is restarted, it should restart from restart-point step2
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_STOP);
        System.out.printf("Restart with params %s%n", params);
        final long jobExecutionId = jobOperator.restart(previousJobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_STOP, jobExecution.getExitStatus());

        Assert.assertEquals(1, jobExecution.getStepExecutions().size());
        // <stop> element does not affect the already-completed step batchlet execution.
        // Although the job STOPPED, but step2 still COMPLETED
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        // step2 exit status from batchlet1.process method return value, not from <stop exit-status> element
        Assert.assertEquals(Batchlet1.ACTION_STOP, jobExecution.getStepExecutions().get(0).getExitStatus());
        return jobExecutionId;
    }
}
