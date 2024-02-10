/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.spi.PropertyKey;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.jberet.se.test.Batchlet1Test.createParams;
import static org.junit.Assert.assertEquals;

/**
 * This test verifies the job data in jdbc job repository from executing the previous test class Batchlet1Test.
 * This test class is configured in pom.xml surefire plugin to run test in separate JVM to avoid having any in-memory stats.
 *
 * This test is configured to run with jdbc job repository via resources/jberet.properties.
 * This test should also run with mongodb job repository, by modifying resources/jberet.properties (switch 2 properties)
 *
 * @see Batchlet1Test
 */
public class JobDataTest {
    static final String restartPositionJob = "org.jberet.se.test.restartPosition";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    /**
     * Tests job data from running test {@link Batchlet1Test#testBatchlet1()}
     * @throws Exception
     *
     * @see Batchlet1Test#testBatchlet1()
     */
    @Test
    public void testJobData() throws Exception {
        final String stepName = "step2";
        //get the latest JobInstance for job named by jobName
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(Batchlet1Test.jobName, 0, 1);
        assertEquals(1, jobInstances.size());

        final JobInstanceImpl jobInstance = (JobInstanceImpl) jobInstances.get(0);
        System.out.printf("In testJobData, jobInstance: %s, id: %s%n", jobInstance, jobInstance.getInstanceId());

        assertEquals(Batchlet1Test.jobName, jobInstance.getJobName());
        Assert.assertNotEquals(0, jobInstance.getInstanceId());

        final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstance);
        //the job was executed 2 times during Batchlet1Test for the latest job instance
        assertEquals(2, jobExecutions.size());

        final JobExecution jobExecution = jobExecutions.get(1);
        System.out.printf("JobInstance id: %s%n", jobInstance.getInstanceId());

        final BatchStatus batchStatus = jobExecution.getBatchStatus();
        final String exitStatus = jobExecution.getExitStatus();
        final Date createTime = jobExecution.getCreateTime();
        final Date endTime = jobExecution.getEndTime();
        final Date lastUpdatedTime = jobExecution.getLastUpdatedTime();
        final Date startTime = jobExecution.getStartTime();
        final Properties jobParameters = jobExecution.getJobParameters();
        assertEquals(Batchlet1Test.jobName, jobExecution.getJobName());
        Assert.assertNotEquals(0, jobExecution.getExecutionId());
        assertEquals(BatchStatus.STOPPED, batchStatus);
        assertEquals(Batchlet1.ACTION_STOP, exitStatus);
        Assert.assertNotNull(createTime);
        Assert.assertNotNull(lastUpdatedTime);
        Assert.assertNotNull(endTime);
        Assert.assertNotNull(startTime);
        Assert.assertNotNull(jobParameters);
        System.out.printf("JobExecution id: %s, batchStatus: %s, exitStatus: %s, createTime: %s, endTime: %s, lastUpdatedTime: %s, startTime: %s, jobParameters: %s%n",
                jobExecution.getExecutionId(), batchStatus, exitStatus, createTime, endTime, lastUpdatedTime, startTime, jobParameters);

        final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecution.getExecutionId());
        assertEquals(1, stepExecutions.size());
        final StepExecution stepExecution = stepExecutions.get(0);
        final String stepName1 = stepExecution.getStepName();
        final BatchStatus batchStatus1 = stepExecution.getBatchStatus();
        final String exitStatus1 = stepExecution.getExitStatus();
        final Date startTime1 = stepExecution.getStartTime();
        final Date endTime1 = stepExecution.getEndTime();
        final Serializable persistentUserData = stepExecution.getPersistentUserData();
        final Metric[] metrics = stepExecution.getMetrics();

        System.out.printf("StepExecution id: %s, stepName: %s, batchStatus: %s, exitStatus: %s, startTime: %s, endTime: %s, persistentUserData: %s, metrics: %s%n",
                stepExecution.getStepExecutionId(), stepName1, batchStatus1, exitStatus1, startTime1, endTime1, persistentUserData,
                Arrays.toString(metrics));

        Assert.assertNotNull(stepExecution.getStepExecutionId());
        assertEquals(stepName, stepName1);
        assertEquals(BatchStatus.COMPLETED, batchStatus1);
        assertEquals(Batchlet1.ACTION_STOP, exitStatus1);
        Assert.assertNotNull(startTime1);
        Assert.assertNotNull(endTime1);
        //Assert.assertEquals(new Integer(1), persistentUserData);
        assertEquals("Persistent User Data", persistentUserData);
        Assert.assertNotNull(metrics);

        restartJobMatchOther(jobExecution.getExecutionId());
    }

    /**
     * Restarts the job execution stopped in {@link Batchlet1Test#testStopWithRestartPoint()}
     * @throws Exception
     *
     * @see org.jberet.se.test.Batchlet1Test#testStopWithRestartPoint
     */
    @Test
    public void testRestartPositionFromBatchlet2Test() throws Exception {
        final long previousJobExecutionId = getOriginalJobExecutionIdFromFile(Batchlet1Test.jobName2ExecutionIdSaveTo);
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_OTHER);
        System.out.printf("Restart JobExecution %s with params %s%n", previousJobExecutionId, params);
        final long jobExecutionId = jobOperator.restart(previousJobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        assertEquals(2, stepExecutions.size());
        assertEquals("stepC", stepExecutions.get(0).getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getBatchStatus());
        assertEquals(BatchStatus.COMPLETED.name(), stepExecutions.get(0).getExitStatus());

        assertEquals("stepE", stepExecutions.get(1).getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(1).getBatchStatus());
        assertEquals(Batchlet1.ACTION_OTHER, stepExecutions.get(1).getExitStatus());
        Batchlet1Test.jobName2ExecutionIdSaveTo.delete();
    }

    // restart the job execution failed in org.jberet.se.test.Batchlet1Test.testStepFail3Times()
    @Test
    public void testRestartWithLimit() throws Exception {
        final long previousJobExecutionId = getOriginalJobExecutionIdFromFile(Batchlet1Test.jobName3ExecutionIdSaveTo);
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_OTHER);
        System.out.printf("Restart JobExecution %s with params %s%n", previousJobExecutionId, params);
        final long jobExecutionId = jobOperator.restart(previousJobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());
        assertEquals(0, stepExecutions.size());
        Batchlet1Test.jobName3ExecutionIdSaveTo.delete();
    }

    /**
     * Tests the normal restart behavior, without passing
     * {@linkplain PropertyKey#RESTART_POSITION custom restart position}.
     * This restart should start from the previous failed step
     * {@code org.jberet.se.test.restartPosition.step2}.
     *
     * @throws Exception if error
     */
    @Test
    public void testRestartPosition2() throws Exception {
        long jobExecutionId = failStep2();

        //restart
        System.out.printf("About to restart job execution: %s%n", jobExecutionId);
        final Properties params = createParams("failInProcess2", Boolean.FALSE.toString());
        jobExecutionId = jobOperator.restart(jobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        System.out.printf("Step executions in restart: %s%n", stepExecutionsToStepNames(stepExecutions));
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(2, stepExecutions.size());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getBatchStatus());
        assertEquals("org.jberet.se.test.restartPosition.step2", stepExecutions.get(0).getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(1).getBatchStatus());
        assertEquals("org.jberet.se.test.restartPosition.step3", stepExecutions.get(1).getStepName());
    }

    /**
     * Tests restart behavior, passing a {@linkplain PropertyKey#RESTART_POSITION custom restart position}
     * as restart job parameter, to restart the failed job execution at step
     * {@code org.jberet.se.test.restartPosition.step1}.
     * This restart position had completed and was one step before the failed step in the execution sequence.
     * We need to set the step attribute {@code allow-start-if-complete=true} in order to run it
     * in the restart execution.
     *
     * @throws Exception if error
     */
    @Test
    public void testRestartPosition1() throws Exception {
        long jobExecutionId = failStep2(createParams("allowStartIfComplete", Boolean.TRUE.toString()));

        //restart
        System.out.printf("About to restart job execution: %s%n", jobExecutionId);
        final Properties params = createParams("failInProcess2", Boolean.FALSE.toString());
        params.setProperty(PropertyKey.RESTART_POSITION, "org.jberet.se.test.restartPosition.step1");

        jobExecutionId = jobOperator.restart(jobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        System.out.printf("Step executions in restart: %s%n", stepExecutionsToStepNames(stepExecutions));
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(3, stepExecutions.size());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getBatchStatus());
        assertEquals("org.jberet.se.test.restartPosition.step1", stepExecutions.get(0).getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(1).getBatchStatus());
        assertEquals("org.jberet.se.test.restartPosition.step2", stepExecutions.get(1).getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(2).getBatchStatus());
        assertEquals("org.jberet.se.test.restartPosition.step3", stepExecutions.get(2).getStepName());
    }

    /**
     * Tests restart behavior, passing a {@linkplain PropertyKey#RESTART_POSITION custom restart position}
     * as restart job parameter, to restart the failed job execution at step
     * {@code org.jberet.se.test.restartPosition.step3}.
     * This restart position did not get to run in the previous execution sequence.
     * This restart execution will skip the previously failed step
     * {@code org.jberet.se.test.restartPosition.step2}
     *
     * @throws Exception if error
     */
    @Test
    public void testRestartPosition3() throws Exception {
        long jobExecutionId = failStep2();

        //restart
        System.out.printf("About to restart job execution: %s%n", jobExecutionId);
        final Properties params = createParams("failInProcess2", Boolean.FALSE.toString());
        params.setProperty(PropertyKey.RESTART_POSITION, "org.jberet.se.test.restartPosition.step3");

        jobExecutionId = jobOperator.restart(jobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        System.out.printf("Step executions in restart: %s%n", stepExecutionsToStepNames(stepExecutions));
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(1, stepExecutions.size());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getBatchStatus());
        assertEquals("org.jberet.se.test.restartPosition.step3", stepExecutions.get(0).getStepName());
    }

    /**
     * Starts the job {@link #restartPositionJob}, and fails the 2nd step.
     *
     * @param p optional job parameters
     * @return the job execution id
     * @throws Exception
     */
    private long failStep2(final Properties... p) throws Exception {
        final Properties params;

        if (p.length > 0) {
            params = p[0];
            params.setProperty("failInProcess2", Boolean.TRUE.toString());
        } else {
            params = Batchlet1Test.createParams("failInProcess2", Boolean.TRUE.toString());
        }

        final long jobExecutionId = jobOperator.start(restartPositionJob, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(2, stepExecutions.size());
        assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getBatchStatus());
        assertEquals(BatchStatus.FAILED, stepExecutions.get(1).getBatchStatus());
        return jobExecutionId;
    }

    /**
     * Retrieves the job execution id of the previously failed or stopped job execution for restarting purpose.
     * This method will cause the jdbc repository to load from database relevant JobExecution and JobInstance.
     * To test cold restart more completely, getOriginalJobExecutionId(File file) method should be used to avoid
     * the pre-fetching.
     * @param jobName the name of the job that previously failed or stopped
     * @return a job execution id used for restart
     */
    private long getOriginalJobExecutionId(final String jobName) {
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, 0, 1);
        final JobInstance jobInstance = jobInstances.get(0);
        final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstance);
        final JobExecution originalJobExecution = jobExecutions.get(jobExecutions.size() - 1);
        return originalJobExecution.getExecutionId();
    }

    /**
     * Retrieves the job execution id of the previously failed or stopped job execution for restarting purpose.
     * @param file a file that contains the job execution id to be retrieved
     * @return a job execution id used for restart
     * @throws IOException
     */
    private long getOriginalJobExecutionIdFromFile(final File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            final String s = br.readLine();
            return Long.parseLong(s);
        } finally {
            try {
                br.close();
            } catch (final IOException e) {
                //ignore
            }
        }
    }

    private long restartJobMatchOther(final long previousJobExecutionId) throws Exception {
        //restart the job and run to complete, not matching any transition elements in step2.
        final Properties params = createParams(Batchlet1.ACTION, Batchlet1.ACTION_OTHER);
        System.out.printf("Restart JobExecution %s with params %s%n", previousJobExecutionId, params);
        final long jobExecutionId = jobOperator.restart(previousJobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(Batchlet1Test.waitTimeoutMinutes, TimeUnit.MINUTES);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        assertEquals(1, jobExecution.getStepExecutions().size());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        assertEquals(Batchlet1.ACTION_OTHER, jobExecution.getStepExecutions().get(0).getExitStatus());
        return jobExecutionId;
    }

    private static List<String> stepExecutionsToStepNames(final List<StepExecution> stepExecutions) {
        if (stepExecutions == null) {
            return null;
        }
        final List<String> result = new ArrayList<String>();
        for (final StepExecution e : stepExecutions) {
            result.add(e.getStepName());
        }
        return result;
    }
}
