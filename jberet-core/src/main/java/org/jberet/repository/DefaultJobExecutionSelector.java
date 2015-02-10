/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.repository;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

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

    public DefaultJobExecutionSelector(final Boolean excludeRunningJobExecutions) {
        if (excludeRunningJobExecutions == Boolean.FALSE) {
            this.excludeRunningJobExecutions = Boolean.FALSE;
        }
    }

    @Override
    public boolean select(final JobExecution jobExecution, final Collection<JobExecution> allJobExecutions) {
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
            for (final JobExecution je : allJobExecutions) {
                if (je.getExecutionId() > id) {
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
