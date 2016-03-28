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

package org.jberet.samples.wildfly.schedule.timer;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.ejb.ScheduleExpression;

import org.jberet.rest.client.BatchClient;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ScheduleTimerIT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/executor-scheduler-job1.xml}
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
    private static final int delaylMinute = 1;
    private static final long sleepTimeMillis = initialDelayMinute * 60 * 1000 + 3000;

    private BatchClient batchClient = new BatchClient(restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void singleActionInitialDelay() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "singleActionInitialDelay");
        final JobScheduleConfig scheduleConfig = new JobScheduleConfig(jobName, 0, params, null, initialDelayMinute, 0, 0);
        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(null, schedule);

//        once an ejb timer expires, it is removed from ejb timer service.
//
//        assertEquals(JobSchedule.Status.DONE, schedule.getStatus());
//        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();

//        final JobExecution jobExecution = batchClient.getJobExecution(jobExecutionIds.get(0));
//        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
//        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
//        System.out.printf("exit status: %s%n", jobExecution.getExitStatus());
    }

    @Test
    public void scheduleExpression1() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleExpression1");
        final ScheduleExpression exp = new ScheduleExpression();
        exp.hour("*").minute("*").second("0/20");

        final JobScheduleConfig scheduleConfig =
                new JobScheduleConfig(jobName, 0, params, exp, 0, 0, 0);
        JobSchedule schedule = batchClient.schedule(scheduleConfig);
        assertEquals(JobSchedule.Status.SCHEDULED, batchClient.getJobSchedule(schedule.getId()).getStatus());
        System.out.printf("Scheduled job schedule: %s%n", schedule.getId());

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        assertEquals(JobSchedule.Status.SCHEDULED, schedule.getStatus());
        System.out.printf("Job executions from above schedule: %s%n", schedule.getJobExecutionIds());
        assertEquals(true, schedule.getJobExecutionIds().size() >= 2);

        final boolean cancelled = batchClient.cancelJobSchedule(schedule.getId());
        assertEquals(true, cancelled);
    }

    @Test
    public void scheduleInterval() throws Exception {
        final Properties params = new Properties();
        params.setProperty(testNameKey, "scheduleInterval");
        final JobScheduleConfig scheduleConfig =
                new JobScheduleConfig(jobName, 0, params, null, initialDelayMinute, 0, intervalMinute);
        final JobScheduleConfig scheduleConfig2 =
                new JobScheduleConfig(jobName, 0, params, null, initialDelayMinute * 10, 0, intervalMinute);

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
            cancelSchedule(jobSchedule);
            cancelSchedule(jobSchedule2);
        }
    }

    private void cancelSchedule(final JobSchedule schedule) {
        final String id = schedule.getId();
        batchClient.cancelJobSchedule(id);
        System.out.printf("Cancelled job schedule %s%n", id);
    }
}
