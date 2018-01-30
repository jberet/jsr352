/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
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

import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.schedule._private.ScheduleExecutorLogger;

/**
 * An implementation of {@code javax.batch.api.listener.JobListener} that
 * schedules the next execution of the same job to start some time after
 * the current execution ends.
 * <p>
 * If there are multiple job listeners in a job, this class should always
 * be specified as the last one within &lt;listeners&gt;.
 *
 * @since 1.3.0
 */
@Named
public class SchedulingJobListener extends AbstractJobListener {
    @Inject
    protected JobContext jobContext;

    /**
     * The initial delay (in minutes) after the current job execution ends,
     * before the next scheduled job execution starts.
     * If set to negative number, no job execution will be scheduled.
     * Optional property, and defaults to 2 minutes.
     */
    @Inject
    @BatchProperty
    protected long initialDelay = 2;

    /**
     * Whether the job schedule is persistent.
     * Note that some underlying job scheduler does not support persistent schedule,
     * and in this case, this property will be ignored.
     * Optional property, and defaults to false.
     */
    @Inject
    @BatchProperty
    protected boolean persistent;

    /**
     * The date and time after which to stop scheduling any further job executions.
     * This property can be used to stop endless job schedules and executions.
     * Optional property, and defaults to null (no stop time).
     */
    @Inject
    @BatchProperty
    protected Date stopAfterTime;

    /**
     * The list of {@code BatchStatus}, if any of which matches the batch status of
     * the current job execution, the next job execution is scheduled. Otherwise,
     * no execution is scheduled.
     */
    @Inject
    @BatchProperty
    protected List<BatchStatus> onBatchStatus;

    /**
     * {@inheritDoc}
     * <p>
     * This method schedules the next execution of the same job to start
     * after the current job execution ends. The same job parameters are
     * carried over to the next scheduled job execution.
     */
    @Override
    public void afterJob() {
        if(needToSchedule()) {
            final JobScheduler scheduler = JobScheduler.getJobScheduler();
            final long executionId = jobContext.getExecutionId();
            final JobExecution jobExecution = JobScheduler.getJobOperator().getJobExecution(executionId);
            final Properties jobParameters = jobExecution.getJobParameters();

            JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                    .jobName(jobContext.getJobName())
                    .initialDelay(initialDelay)
                    .persistent(persistent)
                    .jobParameters(jobParameters)
                    .build();

            final JobSchedule schedule = scheduler.schedule(scheduleConfig);
            ScheduleExecutorLogger.LOGGER.scheduledNextExecution(executionId, schedule.getId(), scheduleConfig);
        }
    }

    /**
     * Checks if need to schedule the next job execution.
     * This method may be overridden by subclass to customize the condition whether to schedule
     * the next job execution.  In this case, the overriding method should class
     * {@code super.needToSchedule() first}.
     *
     * @return false if {@link #initialDelay} is negative, or {@link #stopAfterTime} has passed,
     *          or current batch status does not match any one of {@link #onBatchStatus};
     *          otherwise, return true
     */
    protected boolean needToSchedule() {
        if (initialDelay < 0) {
            return false;
        }
        if (stopAfterTime != null && System.currentTimeMillis() >= stopAfterTime.getTime() ) {
            return false;
        }
        if (onBatchStatus != null && !onBatchStatus.isEmpty()) {
            final BatchStatus currentStatus = jobContext.getBatchStatus();
            if (currentStatus == BatchStatus.STARTED) {
                return onBatchStatus.contains(BatchStatus.COMPLETED) || onBatchStatus.contains(BatchStatus.STARTED);
            } else {
                return onBatchStatus.contains(currentStatus);
            }
        }
        return true;
    }
}
