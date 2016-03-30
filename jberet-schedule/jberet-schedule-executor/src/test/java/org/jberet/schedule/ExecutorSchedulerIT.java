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

@Ignore("These tests each take several minutes to complete, so ignore them from default build")
public class ExecutorSchedulerIT {
    private static final String jobName = "executor-scheduler-job1";
    private static final String testNameKey = "testName";
    private static final int initialDelayMinute = 1;
    private static final int intervalMinute = 1;
    private static final int afterDelaylMinute = 1;
    private static final long sleepTimeMillis = initialDelayMinute * 60 * 1000 + 3000;

    private final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

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

    @Test
    public void delayRepeat() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "delayRepeat");

        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName(jobName)
                .jobParameters(params)
                .initialDelay(initialDelayMinute)
                .afterDelay(afterDelaylMinute)
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
