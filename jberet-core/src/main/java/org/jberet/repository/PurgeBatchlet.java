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

import java.util.Date;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import org.jberet.job.model.Job;
import org.jberet.runtime.context.JobContextImpl;

public final class PurgeBatchlet implements Batchlet {
    @Inject
    private JobContext jobContext;

    @Inject
    @BatchProperty
    boolean purgeAllJobs;

    @Inject
    @BatchProperty
    Set<Long> jobExecutionIds;

    @Inject
    @BatchProperty
    Integer numberOfRecentJobExecutionsToKeep;

    @Inject
    @BatchProperty
    Long jobExecutionIdFrom;

    @Inject
    @BatchProperty
    Long jobExecutionIdTo;

    @Inject
    @BatchProperty
    Integer withinPastMinutes;

    @Inject
    @BatchProperty
    Date jobExecutionEndTimeFrom;

    @Inject
    @BatchProperty
    Date jobExecutionEndTimeTo;

    @Inject
    @BatchProperty
    Set<String> batchStatuses;

    @Inject
    @BatchProperty
    Set<String> exitStatuses;

    @Inject
    @BatchProperty
    Set<String> jobNames;

    @Override
    public String process() throws Exception {
        final JobContextImpl jobContextImpl = (JobContextImpl) jobContext;
        final JobRepository jobRepository = jobContextImpl.getJobRepository();

        if (purgeAllJobs) {
            for (final Job job : jobRepository.getJobs()) {
                jobRepository.removeJob(job.getId());
            }
        } else {
            final DefaultJobExecutionSelector selector = new DefaultJobExecutionSelector();
            selector.jobExecutionIds = jobExecutionIds;
            selector.numberOfRecentJobExecutionsToExclude = numberOfRecentJobExecutionsToKeep;
            selector.jobExecutionIdFrom = jobExecutionIdFrom;
            selector.jobExecutionIdTo = jobExecutionIdTo;
            selector.withinPastMinutes = withinPastMinutes;
            selector.jobExecutionEndTimeFrom = jobExecutionEndTimeFrom;
            selector.jobExecutionEndTimeTo = jobExecutionEndTimeTo;
            selector.batchStatuses = batchStatuses;
            selector.exitStatuses = exitStatuses;
            selector.jobNames = jobNames;

            jobRepository.removeJobExecutions(selector);
        }

        return null;
    }

    @Override
    public void stop() throws Exception {

    }
}
