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
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.jberet.runtime.context.JobContextImpl;

public final class PurgeBatchlet implements Batchlet {
    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Inject
    @BatchProperty
    String sql;

    @Inject
    @BatchProperty
    String sqlFile;

    @Inject
    @BatchProperty
    String mongoRemoveQueries;

    @Inject
    @BatchProperty
    Class jobExecutionSelector;

    @Inject
    @BatchProperty
    Boolean keepRunningJobExecutions;

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
    Set<String> jobExecutionsByJobNames;

    @Inject
    @BatchProperty
    Set<String> purgeJobsByNames;

    @Override
    public String process() throws Exception {
        final JobContextImpl jobContextImpl = (JobContextImpl) jobContext;
        final JobRepository jobRepository = jobContextImpl.getJobRepository();

        if (purgeJobsByNames != null && !purgeJobsByNames.isEmpty()) {
            final boolean purgeAll = purgeJobsByNames.size() == 1 && purgeJobsByNames.contains("*");
            final String currentJobName = jobContext.getJobName();
            for (final String n : jobRepository.getJobNames()) {
                //do not remove the current running job if purgeJobsByNames = "*"
                if (purgeJobsByNames.contains(n) ||
                        (purgeAll && !currentJobName.equals(n))) {
                    jobRepository.removeJob(n);
                }
            }
        } else {
            final JobExecutionSelector selector;
            if (jobExecutionSelector != null) { //use the custom selector configured by the application
                selector = (JobExecutionSelector) jobExecutionSelector.newInstance();
            } else {
                final DefaultJobExecutionSelector selector1 = new DefaultJobExecutionSelector(keepRunningJobExecutions);
                selector1.jobExecutionIds = jobExecutionIds;
                selector1.numberOfRecentJobExecutionsToExclude = numberOfRecentJobExecutionsToKeep;
                selector1.jobExecutionIdFrom = jobExecutionIdFrom;
                selector1.jobExecutionIdTo = jobExecutionIdTo;
                selector1.withinPastMinutes = withinPastMinutes;
                selector1.jobExecutionEndTimeFrom = jobExecutionEndTimeFrom;
                selector1.jobExecutionEndTimeTo = jobExecutionEndTimeTo;
                selector1.batchStatuses = batchStatuses;
                selector1.exitStatuses = exitStatuses;
                selector1.jobExecutionsByJobNames = jobExecutionsByJobNames;
                selector = selector1;
            }
            selector.setJobContext(jobContext);
            selector.setStepContext(stepContext);
            jobRepository.removeJobExecutions(selector);
        }

        if (jobRepository instanceof JdbcRepository) {
            if (sql != null) {
                ((JdbcRepository) jobRepository).executeStatements(sql, null);
            } else if (sqlFile != null) {
                ((JdbcRepository) jobRepository).executeStatements(null, sqlFile);
            }
        } else if (jobRepository instanceof MongoRepository) {
            if (mongoRemoveQueries != null) {
                ((MongoRepository) jobRepository).executeRemoveQueries(mongoRemoveQueries);
            }
        }

        return null;
    }

    @Override
    public void stop() throws Exception {

    }
}
