/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.schedule;

import java.util.List;
import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for batch job scheduling in Java SE environment,
 * where the job scheduler impl {@link ExecutorSchedulerImpl} will be used.
 * <p>
 * A similar set of tests for Java EE, JBoss EAP and WildFly environment are in
 * wildfly-jberet-samples/scheduleExecutor/src/test/java/org/jberet/samples/wildfly/schedule/executor/ScheduleExecutorIT.java
 */
@Ignore("These tests each take several minutes to complete, so ignore them from default build")
public class ExecutorSchedulerIT {
    private static final String jobName = "executor-scheduler-job1";
    private static final String testNameKey = "testName";

    /**
     * Number of minutes for a job schedule.
     * @see JobScheduleConfig#initialDelay
     */
    private static final int initialDelayMinute = 1;

    /**
     * Number of minutes for interval or period in a repeatable job schedule.
     * @see JobScheduleConfig#interval
     */
    private static final int intervalMinute = 1;

    /**
     * Number of minutes for subsequent delay in a repeatable job schedule.
     * @see JobScheduleConfig#afterDelay
     */
    private static final int afterDelayMinute = 1;

    /**
     * Number of milliseconds to sleep, to wait for the job schedule to run.
     * Currently set to 3 seconds longer than {@link #initialDelayMinute}.
     */
    private static final long sleepTimeMillis = initialDelayMinute * 60 * 1000 + 3000;

    private final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

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

        JobSchedule schedule = jobScheduler.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());

        Thread.sleep(sleepTimeMillis);
        schedule = jobScheduler.getJobSchedule(schedule.getId());
        assertEquals(JobSchedule.Status.DONE, schedule.getStatus());
        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();

        final JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionIds.get(0));
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("jobScheduler: %s%n", jobScheduler);
        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
        System.out.printf("exit status: %s%n", jobExecution.getExitStatus());
    }

    /**
     * Tests repeatable job schedule specified with an initial delay and an interval or period.
     * Verifies job schedule status after certain sleep time.
     * Cancels the above schedule, and verifies the cancelled status of the schedule.
     * Verifies the number of realized job executions.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void interval() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "interval");
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .interval(intervalMinute)
                .build();
        JobSchedule schedule = jobScheduler.schedule(scheduleConfig);

        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());

        Thread.sleep(sleepTimeMillis * 2);
        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());

        final boolean cancelStatus = jobScheduler.cancel(schedule.getId());
        assertEquals(true, cancelStatus);
        assertEquals(JobSchedule.Status.CANCELLED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("interval schedule successfully cancelled: %s%n", schedule);

        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();
        assertEquals(2, jobExecutionIds.size());
        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
    }

    /**
     * Tests repeatable job schedule specified with an initial delay and a subsequent delay.
     * Verifies job schedule status after certain sleep time.
     * Cancels the above schedule, and verifies the cancelled status of the schedule.
     * Verifies the number of realized job executions.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void delayRepeat() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "delayRepeat");

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .afterDelay(afterDelayMinute)
                .build();
        JobSchedule schedule = jobScheduler.schedule(scheduleConfig);

        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());

        Thread.sleep(sleepTimeMillis * 2);
        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());

        final boolean cancelStatus = jobScheduler.cancel(schedule.getId());
        assertEquals(true, cancelStatus);
        assertEquals(JobSchedule.Status.CANCELLED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("delayRepeat schedule successfully cancelled: %s%n", schedule);

        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();
        assertEquals(2, jobExecutionIds.size());
        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
    }

    /**
     * Tests cancelling a job schedule.
     * A single-action job schedule is submitted and cancelled immediately.
     * Verifies the job schedule is cancelled successfully, and no job executions
     * have realized.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void cancel() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "cancel");

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .build();
        JobSchedule schedule = jobScheduler.schedule(scheduleConfig);

        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());

        final boolean cancelResult = jobScheduler.cancel(schedule.getId());
        assertEquals(JobSchedule.Status.CANCELLED, jobScheduler.getJobSchedule(schedule.getId()).getStatus());
        assertEquals(true, cancelResult);

        Thread.sleep(sleepTimeMillis);

        try {
            final int jobInstanceCount = jobOperator.getJobInstanceCount(jobName);
            assertEquals(0, jobInstanceCount);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

}
