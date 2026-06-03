/*
 * Copyright (c) 2015-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;

/**
 * Default implementation of {@link JobExecutionSelector}.
 *
 * @since 1.1.0
 */
public final class DefaultJobExecutionSelector implements JobExecutionSelector {
    private JobContext jobContext;
    private StepContext stepContext;

    Boolean excludeRunningJobExecutions = Boolean.TRUE;

    Set<Long> jobExecutionIds;

    Integer numberOfRecentJobExecutionsToExclude;

    Long jobExecutionIdFrom;
    Long jobExecutionIdTo;

    Integer withinPastMinutes;

    Date jobExecutionEndTimeFrom;
    Date jobExecutionEndTimeTo;

    Set<String> batchStatuses;

    Set<String> exitStatuses;

    Set<String> jobExecutionsByJobNames;

    /**
     * Constructs {@code DefaultJobExecutionSelector} with
     * {@code excludeRunningJobExecutions} Boolean flag.
     *
     * @param excludeRunningJobExecutions if true, this selector will not operate on any running
     *                                    job executions, and this selector's {@link #select(JobExecution, Collection)}
     *                                    method will return false for any running job executions. This is the default
     *                                    behavior.
     *                                    If false, running job executions will be treated no differently than other
     *                                    job executions.
     */
    public DefaultJobExecutionSelector(final Boolean excludeRunningJobExecutions) {
        if (Boolean.FALSE.equals(excludeRunningJobExecutions)) {
            this.excludeRunningJobExecutions = Boolean.FALSE;
        }
    }

    @Override
    public boolean select(final JobExecution jobExecution, final Collection<Long> allJobExecutionIds) {
        if (excludeRunningJobExecutions) {
            final BatchStatus batchStatus = jobExecution.getBatchStatus();
            if (batchStatus != BatchStatus.COMPLETED &&
                    batchStatus != BatchStatus.FAILED &&
                    batchStatus != BatchStatus.STOPPED &&
                    batchStatus != BatchStatus.ABANDONED) {
                return false;
            }
        }

        final long id = jobExecution.getExecutionId();
        if (jobExecutionIds != null && !jobExecutionIds.isEmpty()) {
            return jobExecutionIds.contains(id);
        }

        if (numberOfRecentJobExecutionsToExclude != null) {
            int numOfLargerIds = 0;
            for (final Long jeid : allJobExecutionIds) {
                if (jeid > id) {
                    numOfLargerIds++;
                    if (numOfLargerIds >= numberOfRecentJobExecutionsToExclude) {
                        return true;
                    }
                }
            }
            if (numOfLargerIds < numberOfRecentJobExecutionsToExclude) {
                return false;
            }
        }

        if (jobExecutionIdFrom != null) {
            if (jobExecutionIdTo != null) {
                return id >= jobExecutionIdFrom && id <= jobExecutionIdTo;
            } else {
                return id >= jobExecutionIdFrom;
            }
        } else if (jobExecutionIdTo != null) {
            return id <= jobExecutionIdTo;
        }

        final Date endTime = jobExecution.getEndTime();
        // End time may be null if there are unfinished batch jobs
        if (endTime != null) {
            if (withinPastMinutes != null) {
                final long diffMillis = System.currentTimeMillis() - endTime.getTime();
                final long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
                return diffMinutes <= withinPastMinutes;
            }

            if (jobExecutionEndTimeFrom != null) {
                if (jobExecutionEndTimeTo != null) {
                    return (endTime.after(jobExecutionEndTimeFrom) || endTime.equals(jobExecutionEndTimeFrom)) &&
                            (endTime.before(jobExecutionEndTimeTo) || endTime.equals(jobExecutionEndTimeTo));
                } else {
                    return endTime.after(jobExecutionEndTimeFrom) || endTime.equals(jobExecutionEndTimeFrom);
                }
            } else if (jobExecutionEndTimeTo != null) {
                return endTime.before(jobExecutionEndTimeTo) || endTime.equals(jobExecutionEndTimeTo);
            }
        }

        if (batchStatuses != null) {
            return batchStatuses.contains(jobExecution.getBatchStatus().name());
        }

        if (exitStatuses != null) {
            return exitStatuses.contains(jobExecution.getExitStatus());
        }

        if (jobExecutionsByJobNames != null) {
            return jobExecutionsByJobNames.contains(jobExecution.getJobName());
        }

        return false;
    }

    @Override
    public JobContext getJobContext() {
        return jobContext;
    }

    @Override
    public void setJobContext(final JobContext jobContext) {
        this.jobContext = jobContext;
    }

    @Override
    public StepContext getStepContext() {
        return stepContext;
    }

    @Override
    public void setStepContext(final StepContext stepContext) {
        this.stepContext = stepContext;
    }
}
