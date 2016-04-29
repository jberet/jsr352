/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.schedule.executor;

import java.util.List;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.jberet.schedule.ExecutorSchedulerImpl;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for batch job scheduling in Java EE, JBoss EAP and WildFly environment,
 * where the job scheduler impl {@link ExecutorSchedulerImpl} will be used.
 * <p>
 * A similar set of tests using {@code org.jberet.schedule.TimerSchedulerBean} are in
 * wildfly-jberet-samples/scheduleTimer/src/test/java/org/jberet/samples/wildfly/schedule/timer/ScheduleTimerIT.java
 * <p>
 * A similar set of tests for Java SE environment are in
 * jberet-schedule/jberet-schedule-executor/src/test/java/org/jberet/schedule/ExecutorSchedulerIT.java
 */
public final class ScheduleExecutorIT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/executor-scheduler-job1.xml}
     */
    private static final String jobName = "executor-scheduler-job1";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "scheduleExecutor/api";

    private static final String testNameKey = "testName";
    private static final int initialDelayMinute = 1;
    private static final int intervalMinute = 1;
    private static final long sleepTimeMillis = initialDelayMinute * 60 * 1000 + 3000;

    private BatchClient batchClient = new BatchClient(restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    /**
     * Tests single-action job schedule specified with an initial delay.
     * Verifies job schedule status and realized job execution status after
     * sleeping for {@link #sleepTimeMillis}.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void singleActionInitialDelay() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "singleActionInitialDelay");
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .build();

        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(JobSchedule.Status.DONE, schedule.getStatus());
        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();

        final JobExecution jobExecution = batchClient.getJobExecution(jobExecutionIds.get(0));
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
        System.out.printf("exit status: %s%n", jobExecution.getExitStatus());
    }

    /**
     * Tests repeatable job schedule specified with an initial delay and an interval or period.
     * Verifies job schedule status after certain sleep time.
     * Verifies the number of realized job executions.
     * Cancels the above schedule, and verifies the cancelled status of the schedule.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void scheduleInterval() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleInterval");
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .interval(intervalMinute)
                .build();

        JobSchedule jobSchedule = batchClient.schedule(scheduleConfig);
        System.out.printf("Scheduled job schedule %s: %s%n", jobSchedule.getId(), jobSchedule);
        Thread.sleep(sleepTimeMillis * 2);

        jobSchedule = batchClient.getJobSchedule(jobSchedule.getId());
        assertEquals(JobSchedule.Status.SCHEDULED, jobSchedule.getStatus());
        assertEquals(2, jobSchedule.getJobExecutionIds().size());
        assertEquals(BatchStatus.COMPLETED,
                batchClient.getJobExecution(jobSchedule.getJobExecutionIds().get(0)).getBatchStatus());
        assertEquals(true, batchClient.cancelJobSchedule(jobSchedule.getId()));
    }

    /**
     * Tests job schedule for restarting a previously failed job execution.
     * After certain sleep time, verifies that the job schedule status is
     * {@code DONE}, the restarted job execution has batch status {@code COMPLETED}.
     * Tries to cancel the job schedule, but the cancellation should return false
     * since the job schedule is already DONE.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void scheduleRestart() throws Exception {
        Properties params = new Properties();
        params.setProperty(testNameKey, "fail");
        JobExecutionEntity jobExecutionEntity = batchClient.startJob(jobName, params);
        Thread.sleep(3000);
        jobExecutionEntity = batchClient.getJobExecution(jobExecutionEntity.getExecutionId());
        Assert.assertEquals(BatchStatus.FAILED, jobExecutionEntity.getBatchStatus());

        params = new Properties();
        params.setProperty(testNameKey, "scheduleRestart");
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobExecutionId(jobExecutionEntity.getExecutionId())
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .build();

        JobSchedule jobSchedule = batchClient.schedule(scheduleConfig);
        System.out.printf("Scheduled restart job schedule %s: %s%n", jobSchedule.getId(), jobSchedule);
        Thread.sleep(sleepTimeMillis);

        jobSchedule = batchClient.getJobSchedule(jobSchedule.getId());
        assertEquals(JobSchedule.Status.DONE, jobSchedule.getStatus());
        assertEquals(1, jobSchedule.getJobExecutionIds().size());
        assertEquals(BatchStatus.COMPLETED,
                batchClient.getJobExecution(jobSchedule.getJobExecutionIds().get(0)).getBatchStatus());
        assertEquals(false, batchClient.cancelJobSchedule(jobSchedule.getId()));
    }
}
