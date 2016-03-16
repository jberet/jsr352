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
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExecutorSchedulerIT {
    private static final String jobName = "executor-scheduler-job1";
    private static final String testNameKey = "testName";

    private final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Test
    public void singleAction() throws Exception {
        List<JobSchedule> schedules = jobScheduler.getJobSchedules();
        assertEquals(0, schedules.size());

        final Properties params = new Properties();
        params.setProperty(testNameKey, "singleAction");
        final JobScheduleConfig info = new JobScheduleConfig(jobName, 0, params, null, 1, 0, 0);
        JobSchedule schedule = jobScheduler.schedule(info);

        assertEquals(JobSchedule.Status.SCHEDULED, jobScheduler.getStatus(schedule.getId()));

        schedules = jobScheduler.getJobSchedules();
        assertEquals(1, schedules.size());
        assertEquals(schedule, schedules.get(0));

        Thread.sleep(60 * 1000 + 1000);
        schedule = jobScheduler.getJobSchedules().get(0);
        assertEquals(JobSchedule.Status.DONE, schedule.getStatus());
        final List<Long> jobExecutionIds = schedule.getJobExecutionIds();

        final JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionIds.get(0));
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("jobScheduler: %s%n", jobScheduler);
        System.out.printf("jobExecutionIds from scheduled job: %s%n", jobExecutionIds);
        System.out.printf("exit status: %s%n", jobExecution.getExitStatus());
    }


}
