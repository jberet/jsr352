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

import java.util.List;
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
    private static final String listenerJobName = "org.jberet.se.test.sleepBatchletListeners";
    private static final String transitionJobName = "org.jberet.se.test.sleepBatchletTransition";
    private static final String transitionEndJobName = "org.jberet.se.test.sleepBatchletTransitionEnd";
    private static final String transitionAttrJobName = "org.jberet.se.test.sleepBatchletTransitionAttr";

    @Test
    @Ignore("It will pass but takes too long")
    public void sleepComplete() throws Exception {
        final int sleepMinutes = 6;
        final Properties params = new Properties();
        params.setProperty("sleep.minutes", String.valueOf(sleepMinutes));
        final long jobExecutionId = operator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(sleepMinutes * 2, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assert.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
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

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.STOPPED, stepExecution.getBatchStatus());
        Assert.assertEquals(SleepBatchlet.INTERRUPTED, stepExecution.getExitStatus());
    }

    /**
     * Verifies that exception from JobListener.beforeJob() method will cause the job to fail.
     * @throws Exception
     */
    @Test
    public void errorBeforeJob() throws Exception {
        final Properties params = new Properties();
        params.setProperty("failBeforeJob", String.valueOf(true));
        final long jobExecutionId = operator.start(listenerJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals("beforeJob afterJob", jobExecution.getExitStatus());

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        Assert.assertEquals(0, stepExecutions.size());
    }

    /**
     * Verifies that exception from JobListener.afterJob() method will cause the job to fail, but the step in the job
     * will remain completed.
     * @throws Exception
     */
    @Test
    public void errorAfterJob() throws Exception {
        final Properties params = new Properties();
        params.setProperty("failAfterJob", String.valueOf(true));
        final long jobExecutionId = operator.start(listenerJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assert.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
    }

    /**
     * Verifies that exception from StepListener.beforeStep() will cause the step and the job to fail.
     * @throws Exception
     */
    @Test
    public void errorBeforeStep() throws Exception {
        final Properties params = new Properties();
        params.setProperty("failBeforeStep", String.valueOf(true));
        final long jobExecutionId = operator.start(listenerJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals("beforeJob beforeStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());
    }

    /**
     * Verifies that exception from StepListener.afterStep() will cause the step and the job to fail, but the step's
     * exit status has already been set by batchlet and should not be affected.
     * @throws Exception
     */
    @Test
    public void errorAfterStep() throws Exception {
        final Properties params = new Properties();
        params.setProperty("failAfterStep", String.valueOf(true));
        final long jobExecutionId = operator.start(listenerJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assert.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
    }

    /**
     * Verifies that beforeJob, beforeStep, afterStep, afterJob methods are invoked when batchlet process() method
     * throws exception.
     * @throws Exception
     */
    @Test
    public void errorInBatchlet() throws Exception {
        final Properties params = new Properties();
        params.setProperty("failInProcess", String.valueOf(true));
        final long jobExecutionId = operator.start(listenerJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());
    }

    @Test
    public void transitionAfterFailedStep() throws Exception {
        final Properties params = new Properties();
        final long jobExecutionId = operator.start(transitionJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());

        stepExecution = jobExecution.getStepExecutions().get(1);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assert.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
    }

    @Test
    public void transitionAttrAfterFailedStep() throws Exception {
        final Properties params = new Properties();
        final long jobExecutionId = operator.start(transitionAttrJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());
    }

    // verifies a step ended with transition element <end> should not transition to the next step. The entire job should terminate.
    @Test
    public void transitionEnd() throws Exception {
        final Properties params = new Properties();
        final long jobExecutionId = operator.start(transitionEndJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assert.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
        Assert.assertEquals(1, jobExecution.getStepExecutions().size());
    }
}
