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

import java.io.Serializable;
import java.util.Properties;
import javax.ejb.ScheduleExpression;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents job schedule configuration, typically passed from the client side
 * to schedule a job.
 *
 * @see JobSchedule
 * @since 1.3.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JobScheduleConfig implements Serializable {
    private static final long serialVersionUID = 7225109864510680914L;

    /**
     * The job XML name for the job schedule to start the job.
     * Either {@code jobName} or {@code jobExecutionId} should be
     * specified, but not both.
     */
    final String jobName;

    /**
     * The id of a job execution for the job schedule to restart it.
     * Either {@code jobName} or {@code jobExecutionId} should be
     * specified, but not both.
     */
    final long jobExecutionId;

    /**
     * The job parameters for starting the job or restarting the job execution.
     */
    final Properties jobParameters;

    /**
     * The schedule expression for calendar-based job schedule.
     *
     * @see "javax.ejb.ScheduleExpression"
     */
    final ScheduleExpression scheduleExpression;

    /**
     * The initial delay (in minutes) of the job schedule.
     * It should not be specified for calendar-based job schedule.
     */
    final long initialDelay;

    /**
     * The subsequent delay (in minutes) of the repeatable job schedule.
     * It should not specified for single action or calendar-based job schedule.
     * For non-calendar repeatable job schedule, either {@code afterDelay}
     * or {@code interval} should be specified, but not both.
     */
    final long afterDelay;

    /**
     * The interval or period (in minutes) for the repeatable job schedule,
     * for example, run the task every 1000 minutes.
     * It should not specified for single action or calendar-based job schedule.
     * For non-calendar repeatable job schedule, either {@code afterDelay}
     * or {@code interval} should be specified, but not both.
     */
    final long interval;

    /**
     * Whether the job schedule is persistent.
     * Some {@link JobScheduler} implementations may not support persistent schedules.
     */
    final boolean persistent;

    /**
     * Default no-arg constructor.
     */
    public JobScheduleConfig() {
        this(null, 0, null, null, 0, 0, 0, false);
    }

    /**
     * Constructs {@code JobScheduleConfig} with parameters.
     *
     * @param jobName job XML name to start
     * @param jobExecutionId job execution id to restart
     * @param jobParameters job parameters for start and restart
     * @param scheduleExpression schedule expression for calendar-based schedule
     * @param initialDelay initial delay
     * @param afterDelay subsequent delay for repeatable job schedule
     * @param interval interval or period for repeatable job schedule
     * @param persistent whether the job schedule is persistent
     */
    JobScheduleConfig(final String jobName,
                             final long jobExecutionId,
                             final Properties jobParameters,
                             final ScheduleExpression scheduleExpression,
                             final long initialDelay,
                             final long afterDelay,
                             final long interval,
                             final boolean persistent) {
        this.jobName = jobName;
        this.jobExecutionId = jobExecutionId;
        this.jobParameters = jobParameters;
        this.scheduleExpression = scheduleExpression;
        this.initialDelay = initialDelay;
        this.afterDelay = afterDelay;
        this.interval = interval;
        this.persistent = persistent;
    }

    /**
     * Determines if this job schedule is repeatable or not.
     *
     * @return true if {@code afterDelay} greater than 0, {@code interval} greater than 0,
     *          or {@code scheduleExpression not null}
     */
    public boolean isRepeating() {
        return afterDelay > 0 || interval > 0 || scheduleExpression != null;
    }

    /**
     * Gets the job XML name for this job schedule.
     * @return job XML name, null if this job schedule is for restarting a job execution
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Gets the job execution id for this job schedule.
     * @return job execution id, 0 if this job schedule is for starting a job
     */
    public long getJobExecutionId() {
        return jobExecutionId;
    }

    /**
     * Gets the job parameters for starting the job or restarting the job execution.
     * @return job parameters, may be empty or null
     */
    public Properties getJobParameters() {
        return jobParameters;
    }

    /**
     * Gets {@code javax.ejb.ScheduleExpression} for calendar-based job schedule.
     * @return schedule expression, may be null
     */
    public ScheduleExpression getScheduleExpression() {
        return scheduleExpression;
    }

    /**
     * Gets the initial delay (in minutes) of this job schedule.
     *
     * @return initial delay, 0 for calendar-based job schedule
     */
    public long getInitialDelay() {
        return initialDelay;
    }

    /**
     * Gets the subsequent delay (in minutes) of this job schedule.
     * Note that subsequent delay does not apply to single action or
     * calendar-based job schedule.
     *
     * @return subsequent delay
     */
    public long getAfterDelay() {
        return afterDelay;
    }

    /**
     * Gets the interval or period (in minutes) of this job schedule.
     * Note that interval does not apply to single action or
     * calendar-based job schedule. For non-calendar repeatable job schedule
     * that has {@link #afterDelay} specified, the interval is ignored.
     *
     * @return interval or period of the job schedule
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Gets whether the job schedule is persistent.
     * Note that some {@link JobScheduler} implementations may not support
     * persistent schedules.
     *
     * @return whether the job schedule is persistent
     */
    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final JobScheduleConfig that = (JobScheduleConfig) o;

        if (jobExecutionId != that.jobExecutionId) return false;
        if (initialDelay != that.initialDelay) return false;
        if (afterDelay != that.afterDelay) return false;
        if (interval != that.interval) return false;
        if (persistent != that.persistent) return false;
        if (jobName != null ? !jobName.equals(that.jobName) : that.jobName != null) return false;
        if (jobParameters != null ? !jobParameters.equals(that.jobParameters) : that.jobParameters != null)
            return false;
        return scheduleExpression != null ? scheduleExpression.equals(that.scheduleExpression) : that.scheduleExpression == null;

    }

    @Override
    public int hashCode() {
        int result = jobName != null ? jobName.hashCode() : 0;
        result = 31 * result + (int) (jobExecutionId ^ (jobExecutionId >>> 32));
        result = 31 * result + (jobParameters != null ? jobParameters.hashCode() : 0);
        result = 31 * result + (scheduleExpression != null ? scheduleExpression.hashCode() : 0);
        result = 31 * result + (int) (initialDelay ^ (initialDelay >>> 32));
        result = 31 * result + (int) (afterDelay ^ (afterDelay >>> 32));
        result = 31 * result + (int) (interval ^ (interval >>> 32));
        result = 31 * result + (persistent ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobScheduleInfo{" +
                "jobName='" + jobName + '\'' +
                ", jobExecutionId=" + jobExecutionId +
                ", jobParameters=" + jobParameters +
                ", initialDelay=" + initialDelay +
                ", afterDelay=" + afterDelay +
                ", interval=" + interval +
                ", persistent=" + persistent +
                ", scheduleExpression='" + scheduleExpression + '\'' +
                '}';
    }
}
