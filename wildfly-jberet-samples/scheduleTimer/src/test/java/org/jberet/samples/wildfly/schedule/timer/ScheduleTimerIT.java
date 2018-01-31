/*
 * Copyright (c) 2016-2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.schedule.timer;

import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;
import javax.ejb.ScheduleExpression;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for batch job scheduling in Java EE, JBoss EAP and WildFly environment,
 * where the job scheduler impl {@link org.jberet.schedule.TimerSchedulerBean} will be used.
 * <p>
 * A similar set of tests using {@link org.jberet.schedule.ExecutorSchedulerImpl} are in
 * wildfly-jberet-samples/scheduleExecutor/src/test/java/org/jberet/samples/wildfly/schedule/executor/ScheduleExecutorIT.java
 * <p>
 * A similar set of tests for Java SE environment are in
 * jberet-schedule/jberet-schedule-executor/src/test/java/org/jberet/schedule/ExecutorSchedulerIT.java
 */
public final class ScheduleTimerIT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/timer-scheduler-job1.xml}
     */
    private static final String jobName = "timer-scheduler-job1";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "scheduleTimer/api";

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
     * Tests job execution scheduled at the end of the current job execution.
     * This tests first cancels all job schedules to avoid left-over schedules.
     *
     * @throws Exception if errors occur
     *
     * @since 1.3.0
     */
    @Test
    public void scheduleAfterEnd() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleAfterEnd");
        params.setProperty("initialDelay", "1");

        DateFormat df = DateFormat.getDateTimeInstance();
        final Date stopDateTime = new Date(System.currentTimeMillis() + 1000*60*3);
        params.setProperty("stopAfterTime", df.format(stopDateTime));

        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(jobName, params);
        Thread.sleep(sleepTimeMillis);
        assertEquals(BatchStatus.COMPLETED, batchClient.getJobExecution(jobExecutionEntity.getExecutionId()).getBatchStatus());

        //manually verify that the next job execution is scheduled to start approximately 1 minute
        //after this job execution completes.
    }

    /**
     * Same as {@link #scheduleAfterEnd()}, except that this test fails the initial job execution,
     * and schedule to restart (not start) the initial job execution.
     *
     * @throws Exception if errors occur
     *
     * @since 1.3.0
     */
    @Test
    public void scheduleAfterEndRestart() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "fail");
        params.setProperty("initialDelay", "1");

        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(jobName, params);
        Thread.sleep(sleepTimeMillis);
        assertEquals(BatchStatus.FAILED, batchClient.getJobExecution(jobExecutionEntity.getExecutionId()).getBatchStatus());

        //manually verify that the next job execution is scheduled to restart approximately 1 minute
        //after this job execution failed.
    }

    /**
     * Tests single-action job scheudle specified with an initial delay.
     * This tests first cancels all job schedules to avoid left-over schedules.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void singleActionInitialDelay() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "singleActionInitialDelay");

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .build();

        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(null, schedule);
    }

    /**
     * Tests repeatable job schedule specified with {@code javax.ejb.ScheduleExpression#second}.
     * This tests first cancels all job schedules to avoid left-over schedules.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void scheduleExpressionSecond() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleExpressionSecond");
        final ScheduleExpression exp = new ScheduleExpression().hour("*").minute("*").second("0/20");

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .scheduleExpression(exp)
                .build();

        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(JobSchedule.Status.SCHEDULED, schedule.getStatus());
        System.out.printf("Job executions from above schedule: %s%n", schedule.getJobExecutionIds());
        assertEquals(true, schedule.getJobExecutionIds().size() >= 2);
        assertEquals(true, cancelJobSchedule(schedule));

        deleteJobSchedule(schedule);
    }

    /**
     * Tests repeatable job schedule specified with {@code javax.ejb.ScheduleExpression#second}
     * and {@code javax.ejb.ScheduleExpression#persistent}.
     *
     * This tests first cancels all job schedules to avoid left-over schedules.
     * @throws Exception if errors occur
     */
    @Test
    public void scheduleExpressionPersistent() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleExpressionPersistent");
        final ScheduleExpression exp = new ScheduleExpression().hour("*").minute("*").second("0/20");

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .scheduleExpression(exp)
                .persistent(true)
                .build();

        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(JobSchedule.Status.SCHEDULED, schedule.getStatus());
        System.out.printf("Job executions from above schedule: %s%n", schedule.getJobExecutionIds());
        assertEquals(true, schedule.getJobExecutionIds().size() >= 2);
        assertEquals(true, cancelJobSchedule(schedule));

        deleteJobSchedule(schedule);
    }

    /**
     * Tests repeatable job schedule specified with
     * {@code javax.ejb.ScheduleExpression#end} date.
     *
     * This tests first cancels all job schedules to avoid left-over schedules.
     * @throws Exception if errors occur
     */
    @Test
    public void scheduleExpressionEnd() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleExpressionEnd");
        final ScheduleExpression exp = new ScheduleExpression().hour("*").minute("*").second("0/20")
                .end(new Date(System.currentTimeMillis() + 60*1000));

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .scheduleExpression(exp)
                .build();

        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        //the job schedule should have ended due to the end attribute in ScheduleExpression
        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(null, schedule);
    }
    /**
     * Tests repeatable job schedule specified with
     * {@code javax.ejb.ScheduleExpression#start} date.
     *
     * This tests first cancels all job schedules to avoid left-over schedules.
     * @throws Exception if errors occur
     */

    @Test
    public void scheduleExpressionStart() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleExpressionStart");

        final ScheduleExpression exp = new ScheduleExpression().hour("*").minute("*").second("0/1")
                .start(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .scheduleExpression(exp)
                .build();

        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        //the job schedule have not started due to the start attribute in ScheduleExpression
        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(0, schedule.getJobExecutionIds().size());
        assertEquals(true, cancelJobSchedule(schedule));

        deleteJobSchedule(schedule);
    }

    /**
     * Tests repeatable job schedule specified with an initial delay and interval.
     * This tests first cancels all job schedules to avoid left-over schedules.
     *
     * @throws Exception if errors occur
     */
    @Test
    public void scheduleInterval() throws Exception {
        cancelAllSchedules();
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleInterval");
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .interval(intervalMinute)
                .build();

        final JobScheduleConfig scheduleConfig2 = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute * 10)
                .interval(intervalMinute)
                .build();

        JobSchedule jobSchedule = batchClient.schedule(scheduleConfig);
        JobSchedule jobSchedule2 = batchClient.schedule(scheduleConfig2);
        System.out.printf("Scheduled job schedule: %s%n", jobSchedule.getId());
        System.out.printf("Scheduled job schedule 2: %s%n", jobSchedule2.getId());
        Thread.sleep(sleepTimeMillis * 2);

        try {
            jobSchedule = batchClient.getJobSchedule(jobSchedule.getId());
            assertEquals(JobSchedule.Status.SCHEDULED, jobSchedule.getStatus());
            assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(jobSchedule2.getId()).getStatus());

            assertEquals(true, batchClient.getJobSchedules().length >= 2);
            assertEquals(2, jobSchedule.getJobExecutionIds().size());
            assertEquals(BatchStatus.COMPLETED,
                    batchClient.getJobExecution(jobSchedule.getJobExecutionIds().get(0)).getBatchStatus());

        } finally {
            deleteJobSchedule(jobSchedule);
            deleteJobSchedule(jobSchedule2);
        }
    }

    /**
     * Cancels the job schedule, logs the cancellation status, and
     * returns the status (true or false).
     *
     * @param schedule the job schedule to cancel
     * @return true if cancelled successfully; false otherwise
     */
    private boolean cancelJobSchedule(final JobSchedule schedule) {
        final String id = schedule.getId();
        final boolean cancelled = batchClient.cancelJobSchedule(id);
        if (cancelled) {
            System.out.printf("Cancelled job schedule %s%n", id);
        } else {
            System.out.printf("Tried to cancel schedule %s, but failed.", id);
        }
        return cancelled;
    }

    /**
     * Retrieves all job schedules and cancel them one by one.
     */
    private void cancelAllSchedules() {
        for (final JobSchedule e : batchClient.getJobSchedules()) {
            cancelJobSchedule(e);
        }
    }

    /**
     * Deletes the job schedule, and verifies that it no longer exists.
     *
     * @param schedule the job schedule to cancel
     */
    private void deleteJobSchedule(final JobSchedule schedule) {
        final String id = schedule.getId();
        batchClient.deleteJobSchedule(id);
        assertEquals(null, batchClient.getJobSchedule(id));
    }
}
