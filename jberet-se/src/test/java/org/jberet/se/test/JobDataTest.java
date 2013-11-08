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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test verifies the job data in jdbc job repository from executing the previous test class Batchlet1Test.
 * This test class itself does not start any job, and is configured in pom.xml surefire plugin to run test in
 * separate JVM to avoid having any in-memory stats.
 */
public class JobDataTest {
    private static final String jobName = "batchlet1";
    private static final String stepName = "step2";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Test
    public void testJobData() throws Exception {
        //get the latest JobInstance for job named by jobName
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, 0, 1);
        Assert.assertEquals(1, jobInstances.size());

        final JobInstance jobInstance = jobInstances.get(0);
        Assert.assertEquals(jobName, jobInstance.getJobName());
        Assert.assertNotEquals(0, jobInstance.getInstanceId());

        final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstance);
        //the job was executed 2 times during Batchlet1Test for the latest job instance
        Assert.assertEquals(2, jobExecutions.size());

        final JobExecution jobExecution = jobExecutions.get(1);
        System.out.printf("JobInstance id: %s%n", jobInstance.getInstanceId());

        final BatchStatus batchStatus = jobExecution.getBatchStatus();
        final String exitStatus = jobExecution.getExitStatus();
        final Date createTime = jobExecution.getCreateTime();
        final Date endTime = jobExecution.getEndTime();
        final Date lastUpdatedTime = jobExecution.getLastUpdatedTime();
        final Date startTime = jobExecution.getStartTime();
        final Properties jobParameters = jobExecution.getJobParameters();
        Assert.assertEquals(jobName, jobExecution.getJobName());
        Assert.assertNotEquals(0, jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.STOPPED, batchStatus);
        Assert.assertEquals(Batchlet1.ACTION_STOP, exitStatus);
        Assert.assertNotNull(createTime);
        Assert.assertNotNull(lastUpdatedTime);
        Assert.assertNotNull(endTime);
        Assert.assertNotNull(startTime);
        Assert.assertNotNull(jobParameters);
        System.out.printf("JobExecution id: %s, batchStatus: %s, exitStatus: %s, createTime: %s, endTime: %s, lastUpdatedTime: %s, startTime: %s, jobParameters: %s%n",
                jobExecution.getExecutionId(), batchStatus, exitStatus, createTime, endTime, lastUpdatedTime, startTime, jobParameters);

        final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecution.getExecutionId());
        Assert.assertEquals(1, stepExecutions.size());
        final StepExecution stepExecution = stepExecutions.get(0);
        final String stepName1 = stepExecution.getStepName();
        final BatchStatus batchStatus1 = stepExecution.getBatchStatus();
        final String exitStatus1 = stepExecution.getExitStatus();
        final Date startTime1 = stepExecution.getStartTime();
        final Date endTime1 = stepExecution.getEndTime();
        final Serializable persistentUserData = stepExecution.getPersistentUserData();
        final Metric[] metrics = stepExecution.getMetrics();

        Assert.assertNotNull(stepExecution.getStepExecutionId());
        Assert.assertEquals(stepName, stepName1);
        Assert.assertEquals(BatchStatus.COMPLETED, batchStatus1);
        Assert.assertEquals(Batchlet1.ACTION_STOP, exitStatus1);
        Assert.assertNotNull(startTime1);
        Assert.assertNotNull(endTime1);
        //Assert.assertEquals(new Integer(1), persistentUserData);
        Assert.assertEquals("Persistent User Data", persistentUserData);
        Assert.assertNotNull(metrics);
        System.out.printf("StepExecution id: %s, stepName: %s, batchStatus: %s, exitStatus: %s, startTime: %s, endTime: %s, persistentUserData: %s, metrics: %s%n",
                stepExecution.getStepExecutionId(), stepName1, batchStatus1, exitStatus1, startTime1, endTime1, persistentUserData,
                Arrays.toString(metrics));

        restartJobMatchOther(jobExecution.getExecutionId());
    }

    private long restartJobMatchOther(final long previousJobExecutionId) throws Exception {
        //restart the job and run to complete, not matching any elements in step2.
        final Properties params = Batchlet1Test.createParams(Batchlet1.ACTION, Batchlet1.ACTION_OTHER);
        System.out.printf("Restart JobExecution %s with params %s%n", previousJobExecutionId, params);
        final long jobExecutionId = jobOperator.restart(previousJobExecutionId, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT, TimeUnit.SECONDS);
        System.out.printf("JobExecution id: %s%n", jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        Assert.assertEquals(1, jobExecution.getStepExecutions().size());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().get(0).getBatchStatus());
        Assert.assertEquals(Batchlet1.ACTION_OTHER, jobExecution.getStepExecutions().get(0).getExitStatus());
        return jobExecutionId;
    }
}
