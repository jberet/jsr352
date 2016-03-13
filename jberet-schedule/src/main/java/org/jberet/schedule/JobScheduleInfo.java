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

import java.util.Properties;

public final class JobScheduleInfo {

    final String jobName;

    final long jobExecutionId;

    final Properties jobParameters;

    final String scheduleExpression;

    final long delay;

    final long period;

    public JobScheduleInfo(final String jobName,
                           final long jobExecutionId,
                           final Properties jobParameters,
                           final String scheduleExpression,
                           final long delay,
                           final long period) {
        this.jobName = jobName;
        this.jobExecutionId = jobExecutionId;
        this.jobParameters = jobParameters;
        this.scheduleExpression = scheduleExpression;
        this.delay = delay;
        this.period = period;
    }

    /**
     * Result of the job schedule. It can be one of the following values:
     * <ul>
     *     <li>-1: cancelled
     *     <li> 0: scheduled
     *     <li> positive long number: the new job execution id after the job is started
     * </ul>
     */
    long result;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final JobScheduleInfo that = (JobScheduleInfo) o;

        if (jobExecutionId != that.jobExecutionId) return false;
        if (delay != that.delay) return false;
        if (period != that.period) return false;
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
        result = 31 * result + (int) (delay ^ (delay >>> 32));
        result = 31 * result + (int) (period ^ (period >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "JobScheduleInfo{" +
                "jobName='" + jobName + '\'' +
                ", jobExecutionId=" + jobExecutionId +
                ", jobParameters=" + jobParameters +
                ", scheduleExpression='" + scheduleExpression + '\'' +
                ", delay=" + delay +
                ", period=" + period +
                ", result=" + result +
                '}';
    }
}
