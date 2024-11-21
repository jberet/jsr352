/*
 * Copyright (c) 2013-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;

import org.jberet.operations.ForceStopJobOperatorImpl;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.spi.ForceStopJobOperatorContextSelector;
import org.jberet.spi.JobOperatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SleepBatchletTest {
    private final JobOperator operator = BatchRuntime.getJobOperator();
    private static final String jobName = "org.jberet.se.test.sleepBatchlet";
    private static final String listenerJobName = "org.jberet.se.test.sleepBatchletListeners";
    private static final String transitionJobName = "org.jberet.se.test.sleepBatchletTransition";
    private static final String transitionEndJobName = "org.jberet.se.test.sleepBatchletTransitionEnd";
    private static final String transitionAttrJobName = "org.jberet.se.test.sleepBatchletTransitionAttr";

    @Test
    @Disabled("It will pass but takes too long")
    public void sleepComplete() throws Exception {
        final int sleepMinutes = 6;
        final Properties params = new Properties();
        params.setProperty("sleep.minutes", String.valueOf(sleepMinutes));
        final long jobExecutionId = operator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(sleepMinutes * 2, TimeUnit.MINUTES);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assertions.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
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
        System.out.printf("jobExecution id=%s, batchStatus=%s, exitStatus=%s, jobParameters=%s, restartPosition=%s, " +
                        "createTime=%s, startTime=%s, lastUpdateTime=%s, endTime=%s%n",
                jobExecutionId, jobExecution.getBatchStatus(), jobExecution.getExitStatus(), jobExecution.getJobParameters(),
                jobExecution.getRestartPosition(), jobExecution.getCreateTime(), jobExecution.getStartTime(),
                jobExecution.getLastUpdatedTime(), jobExecution.getLastUpdatedTime(), jobExecution.getEndTime());
        Assertions.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.STOPPED.name(), jobExecution.getExitStatus());
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
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals("beforeJob afterJob", jobExecution.getExitStatus());

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        Assertions.assertEquals(0, stepExecutions.size());
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
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assertions.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
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
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());
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
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assertions.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
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
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals("beforeJob beforeStep afterStep afterJob", jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());
    }

    @Test
    public void transitionAfterFailedStep() throws Exception {
        final Properties params = new Properties();
        final long jobExecutionId = operator.start(transitionJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());

        stepExecution = jobExecution.getStepExecutions().get(1);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assertions.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
    }

    @Test
    public void transitionAttrAfterFailedStep() throws Exception {
        final Properties params = new Properties();
        final long jobExecutionId = operator.start(transitionAttrJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), stepExecution.getExitStatus());
    }

    // verifies a step ended with transition element <end> should not transition to the next step. The entire job should terminate.
    @Test
    public void transitionEnd() throws Exception {
        final Properties params = new Properties();
        final long jobExecutionId = operator.start(transitionEndJobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        final StepExecution stepExecution = jobExecution.getStepExecutions().get(0);
        System.out.printf("stepExecution id=%s, name=%s, batchStatus=%s, exitStatus=%s%n",
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution.getBatchStatus());
        Assertions.assertEquals(SleepBatchlet.SLEPT, stepExecution.getExitStatus());
        Assertions.assertEquals(1, jobExecution.getStepExecutions().size());
    }
}
