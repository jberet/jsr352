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

import static javax.batch.runtime.BatchStatus.*;

/**
 * An implementation of {@code javax.batch.api.listener.JobListener} that
 * schedules the next execution of the same job to start some time after
 * the current execution ends.
 * <p>
 * If there are multiple job listeners in a job, this class should always
 * be specified as the last one within &lt;listeners&gt;, so as to
 * minimize the impact on the current job execution.
 *
 * @since 1.3.0
 */
@Named
public class SchedulingJobListener extends AbstractJobListener {
    /**
     * The job parameter key for specifying the max number of schedules
     *
     * @see #maxSchedules
     */
    private static final String numOfSchedulesKey = SchedulingJobListener.class.getName() + ".numOfSchedules";

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
     * This property can be used to prevent endless job schedules and executions.
     * Optional property, and defaults to null (no stop time).
     */
    @Inject
    @BatchProperty
    protected Date stopAfterTime;

    /**
     * The maximum number of schedules that will be preformed by this class.
     * This property can be used to prevent endless job schedules and executions.
     * Optional property, and defaults to null (no limit).
     */
    @Inject
    @BatchProperty
    protected Integer maxSchedules;

    /**
     * The list of {@code BatchStatus}, if any of which matches the batch status of
     * the current job execution, the next job execution is scheduled. Otherwise,
     * no execution is scheduled.
     * Optional property, and defaults to null (current batch status is not considered).
     */
    @Inject
    @BatchProperty
    protected List<BatchStatus> onBatchStatus;

    /**
     * Whether to schedule to restart a failed or stopped job execution, or to
     * start another job execution. This property applies only when the current
     * batch status is {@code FAILED} or {@code STOPPED}. For other statuses,
     * this property is ignored.
     * Optional property, and defaults to true (restart failed or stopped job executions).
     */
    @Inject
    @BatchProperty
    protected boolean restartFailedStopped = true;

    /**
     * {@inheritDoc}
     * <p>
     * This method schedules the next execution of the same job to start
     * after the current job execution ends. The same job parameters are
     * carried over to the next scheduled job execution.
     */
    @Override
    public void afterJob() {
        final long executionId = jobContext.getExecutionId();
        try {
            final JobExecution jobExecution = JobScheduler.getJobOperator().getJobExecution(executionId);
            final BatchStatus currentStatus = jobContext.getBatchStatus();

            if (needToSchedule(jobExecution, currentStatus)) {
                final Properties nextExecutionParams = getJobParameters(jobExecution);

                if (maxSchedules != null) {
                    final Properties currentExecutionParams = jobExecution.getJobParameters();
                    int currentScheduleCount = 0;
                    if (currentExecutionParams != null) {
                        final String numValue = currentExecutionParams.getProperty(numOfSchedulesKey);
                        if (numValue != null) {
                            currentScheduleCount = Integer.parseInt(numValue);
                        }
                    }
                    nextExecutionParams.setProperty(numOfSchedulesKey, String.valueOf(currentScheduleCount + 1));
                }

                final JobScheduler scheduler = JobScheduler.getJobScheduler();
                JobScheduleConfigBuilder builder = JobScheduleConfigBuilder.newInstance()
                        .initialDelay(initialDelay)
                        .persistent(persistent)
                        .jobParameters(nextExecutionParams);
                final JobScheduleConfig scheduleConfig;

                if ((currentStatus == FAILED || currentStatus == STOPPED || currentStatus == STOPPING)
                        && isRestartFailedStopped()) {
                    scheduleConfig = builder.jobExecutionId(executionId).build();
                } else {
                    scheduleConfig = builder.jobName(jobContext.getJobName()).build();
                }

                final JobSchedule schedule = scheduler.schedule(scheduleConfig);

                ScheduleExecutorLogger.LOGGER.scheduledNextExecution(executionId, schedule.getId(), scheduleConfig);
            }
        } catch (final Throwable th) {
            ScheduleExecutorLogger.LOGGER.failToSchedule(th, executionId);
        }
    }

    /**
     * Checks if need to schedule the next job execution.
     * This method may be overridden by subclass to customize the condition whether to schedule
     * the next job execution.  In this case, the overriding method should class
     * {@code super.needToSchedule() first}.
     *
     * @param jobExecution  the current job execution
     * @param currentStatus the batch status of the current job execution
     * @return false if {@link #initialDelay} is negative, or {@link #stopAfterTime} has passed,
     * or current batch status does not match any one of {@link #onBatchStatus},
     * or has exceeded {@link #maxSchedules};
     * otherwise, return true
     */
    protected boolean needToSchedule(final JobExecution jobExecution, final BatchStatus currentStatus) {
        if (initialDelay < 0) {
            return false;
        }
        if (stopAfterTime != null && System.currentTimeMillis() >= stopAfterTime.getTime()) {
            return false;
        }

        if (maxSchedules != null) {
            final Properties jobParameters = jobExecution.getJobParameters();
            if (jobParameters != null) {
                final String numValue = jobParameters.getProperty(numOfSchedulesKey);
                if (numValue != null) {
                    final int num = Integer.parseInt(numValue);
                    if (num >= maxSchedules) {
                        return false;
                    }
                }
            }
        }

        if (onBatchStatus != null && !onBatchStatus.isEmpty()) {
            if (currentStatus == STARTED) {
                return onBatchStatus.contains(COMPLETED) || onBatchStatus.contains(STARTED);
            } else if (currentStatus == STOPPING) {
                return onBatchStatus.contains(STOPPING) || onBatchStatus.contains(STOPPED);
            } else {
                return onBatchStatus.contains(currentStatus);
            }
        }
        return true;
    }

    /**
     * Determines whether to schedule to restart a failed or stopped job execution,
     * or to start another job execution afresh.
     * This method may be overridden by subclasses to implement finer-controlled conditions.
     */
    protected boolean isRestartFailedStopped() {
        return restartFailedStopped;
    }

    /**
     * Gets the job parameters for the next scheduled job execution.
     * This method returns a copy of the job parameters used in the current job execution.
     * This method may be overridden by subclass to include additional job parameters.
     * <p>
     * If so, the overriding method should not modify {@code jobExecution}, or the
     * job parameters obtained from {@code jobExecution}.
     *
     * @param jobExecution the current job execution
     * @return job parameters for the next scheduled job execution, which may be
     *          a copy of the job parameters of the current job execution, or
     *          a totally different {@code java.util.Properties} instance
     */
    protected Properties getJobParameters(final JobExecution jobExecution) {
        final Properties properties = new Properties();
        final Properties currentExecutionParams = jobExecution.getJobParameters();

        if (currentExecutionParams != null) {
            properties.putAll(currentExecutionParams);
        }
        return properties;
    }
}
