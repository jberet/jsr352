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

import org.jberet.rest.client.BatchClient;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.junit.Assert;
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

        Thread.sleep(sleepTimeMillis);
        schedule = batchClient.getJobSchedule(schedule.getId());
        Assert.assertEquals(null, schedule);

//        once an ejb timer expires, it is removed from ejb timer service.
//
//        assertEquals(JobSchedule.Status.DONE, schedule.getStatus());
//        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();

//        final JobExecution jobExecution = batchClient.getJobExecution(jobExecutionIds.get(0));
//        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
//        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
//        System.out.printf("exit status: %s%n", jobExecution.getExitStatus());
    }

}
