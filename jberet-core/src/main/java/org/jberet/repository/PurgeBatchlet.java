/*
 * Copyright (c) 2015-2018 Red Hat, Inc. and/or its affiliates.
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

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.jberet._private.BatchMessages;
import org.jberet.runtime.context.JobContextImpl;

/**
 * A batchlet that removes unwanted job data, such as step executions, job execution,
 * job instances, etc, based on various criteria specified as batch properties.
 * <p>
 * Most batch properties in this class work with all types of batch job repositories.
 * Some batch properties are specific to certain type of job repository. For instance,
 * {@link #sql} and {@link #sqlFile} only work with jdbc job repository,
 * {@link #mongoRemoveQueries} only works with MongoDB job repository.
 */
public class PurgeBatchlet implements Batchlet {
    /**
     * Injected job context of the current job execution.
     */
    @Inject
    protected JobContext jobContext;

    /**
     * Injected step context of the current step execution.
     */
    @Inject
    protected StepContext stepContext;

    /**
     * One or more sql statements for removing certain job data. Multiple sql statements
     * are separated by semi-colon (;). This property is only applicable for jdbc
     * job repository.
     *
     * @see #sqlFile
     */
    @Inject
    @BatchProperty
    protected String sql;

    /**
     * Path to the resource file that contains one or more sql statements for removing
     * certain job data. Multiple sql statements are separated by semi-colon (;). This
     * property is only applicable for jdbc job repository.
     * <p>
     * This property is similar to {@link #sql}, except that the sql statements are in
     * a separate resource file. Therefore, only one of them should be configured in a
     * given step. When both are present, {@link #sql} property is used and {@link #sqlFile}
     * is ignored.
     *
     * @see #sql
     */
    @Inject
    @BatchProperty
    protected String sqlFile;

    /**
     * One or more MongoDB remove queries delimited by semi-colon (;) for removing
     * certain job data.
     * This property is for MongoDB job repository only. For example,
     * <pre>
     *     db.PARTITION_EXECUTION.remove({ STEPEXECUTIONID: { $gt: 100 } });
     *     db.STEP_EXECUTION.remove({ STEPEXECUTIONID: { $gt: 100 } });
     *     db.JOB_EXECUTION.remove({ JOBEXECUTIONID: { $gt: 10 } });
     *     db.JOB_INSTANCE.remove({ JOBINSTANCEID: { $gt: 10 } })
     * </pre>
     */
    @Inject
    @BatchProperty
    protected String mongoRemoveQueries;

    /**
     * Fully-qualified name of a class implementing
     * {@code org.jberet.repository.JobExecutionSelector}, which gives
     * application flexibility in filtering job execution data.
     * <p>
     * If this property is present, other batch properties related to job execution
     * in this class are ignored.
     */
    @Inject
    @BatchProperty
    protected Class jobExecutionSelector;

    /**
     * Whether or not to keep job data belonging to all running job executions.
     * If set to true, job data belonging to all running job executions will not be
     * removed (this is the default behavior).
     * If set to false, any running job executions will be treated the same
     * as finished job executions.
     */
    @Inject
    @BatchProperty
    protected Boolean keepRunningJobExecutions;

    /**
     * Specifies one or more job executions ids, and job data for these job executions
     * will be removed. Multiple values are separate by comma (,). For example,
     * <ul>
     *     <li>100, 90, 200
     *     <li>100
     *     <li>500, 501, 502
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Set<Long> jobExecutionIds;

    /**
     * Specifies the number of most recent job executions to keep, and other
     * job executions will be removed. The order is determined by the job execution
     * id numeric value. For example,
     * <ul>
     *     <li>100
     *     <li>10
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Integer numberOfRecentJobExecutionsToKeep;

    /**
     * Specifies the starting value of job execution id, and all job executions whose id
     * equals to or is greater than this starting value will be removed. This property
     * is typically used along with {@link #jobExecutionIdTo} to form a range of
     * job execution ids. If {@link #jobExecutionIdTo} is not specified, the range
     * is up to the maximum job execution id.
     *
     * @see #jobExecutionIdTo
     */
    @Inject
    @BatchProperty
    protected Long jobExecutionIdFrom;

    /**
     * Specifies the end value of job execution id, and all job executions whose id
     * equals to or is less than this end value will be removed. This property
     * is typically used along with {@link #jobExecutionIdFrom} to form a range of
     * job execution ids. If {@link #jobExecutionIdFrom} is not specified, the range
     * starts from 1.
     *
     * @see #jobExecutionIdFrom
     */
    @Inject
    @BatchProperty
    protected Long jobExecutionIdTo;

    /**
     * Specifies the number of minutes after the end time of a job execution, and
     * any job executions that end within that number of minutes will be removed.
     */
    @Inject
    @BatchProperty
    protected Integer withinPastMinutes;

    /**
     * Specifies the starting value of the end time of a job execution, and any
     * job executions whose end time is equal to or later than that starting value
     * will be removed. This property is typically used along with
     * {@link #jobExecutionEndTimeTo} to form a range of job execution end time.
     * For example,
     * <ul>
     *     <li>05/30/2013 7:03 AM
     *     <li>June 09, 2013 7:03:47 AM PDT
     * </ul>
     *
     * @see #jobExecutionEndTimeTo
     */
    @Inject
    @BatchProperty
    protected Date jobExecutionEndTimeFrom;

    /**
     * Specifies the end value of the end time of a job execution, and any
     * job executions whose end time is equal to or earlier than that end value
     * will be removed. This property is typically used along with
     * {@link #jobExecutionEndTimeFrom} to form a range of job execution end time.
     * For example,
     * <ul>
     *     <li>05/30/2013 7:03 AM
     *     <li>June 09, 2013 7:03:47 AM PDT
     * </ul>
     *
     * @see #jobExecutionEndTimeFrom
     */
    @Inject
    @BatchProperty
    protected Date jobExecutionEndTimeTo;

    /**
     * Specifies one or more batch status values separated by comma (,), and job executions
     * whose batch status matches (case sensitive) any of the specified values will be removed.
     * For example,
     * <ul>
     * <li>FAILED, STOPPED
     * <li>STARTED
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Set<String> batchStatuses;

    /**
     * Specifies one or more exit status values separated by comma (,), and job executions
     * whose exit status matches (case sensitive) any of the specified values will be removed.
     * For example,
     * <ul>
     * <li>fail now, FAIL
     * <li>stop here
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Set<String> exitStatuses;

    /**
     * Specifies one or more job names separated by comma (,), and job executions
     * belonging to any of the specified job names will be removed.
     * For example,
     * <ul>
     *     <li>accountingJob1
     *     <li>BillingJob1, BillingJob2, survey-job-3
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Set<String> jobExecutionsByJobNames;

    /**
     * Specifies one or more job names separated by comma (,), and job information
     * for those job names in the batch runtime will be removed.
     * You can also use the wildcard (*) to represent all job names minus the
     * current job (you cannot remove the job information for the current job).
     * Wildcard can only be used as a single value, and may not be combined with
     * other job names.
     * <p>
     * For example,
     * <ul>
     *     <li>*
     *     <li>accoutingJob1
     *     <li>BillingJob1, BillingJob2, survey-job-3
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Set<String> purgeJobsByNames;

    @Override
    public String process() throws Exception {
        final JobContextImpl jobContextImpl = (JobContextImpl) jobContext;
        final JobRepository jobRepository = jobContextImpl.getJobRepository();

        synchronized (PurgeBatchlet.class) {
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

            if (sql != null) {
                sql = sql.trim();
                if (sql.isEmpty()) {
                    sql = null;
                }
            }
            if (sqlFile != null) {
                sqlFile = sqlFile.trim();
                if (sqlFile.isEmpty()) {
                    sqlFile = null;
                }
            }
            if (sql != null || sqlFile != null) {
                final JdbcRepository jdbcRepository = getJdbcRepository(jobRepository);
                if (jdbcRepository != null) {
                    jdbcRepository.executeStatements(sql, sqlFile);
                }
            }

            if (mongoRemoveQueries != null && jobRepository instanceof MongoRepository) {
                ((MongoRepository) jobRepository).executeRemoveQueries(mongoRemoveQueries);
            }
        }

        return null;
    }

    @Override
    public void stop() throws Exception {
    }

    /**
     * Gets the {@code org.jberet.repository.JdbcRepository} from the
     * {@code org.jberet.repository.JobRepository} passed in, in order to
     * perform operations specific to {@code org.jberet.repository.JdbcRepository}.
     *
     * @param repo {@code JobRepository}
     * @return {@code org.jberet.repository.JdbcRepository}
     */
    protected JdbcRepository getJdbcRepository(final JobRepository repo) {
        if (repo instanceof JdbcRepository) {
            return (JdbcRepository) repo;
        }
        if (repo instanceof InMemoryRepository || repo instanceof MongoRepository
                || repo instanceof InfinispanRepository) {
            return null;
        }

        try {
            final Method getDelegateMethod = repo.getClass().getDeclaredMethod("getDelegate");
            if (!getDelegateMethod.isAccessible()) {
                getDelegateMethod.setAccessible(true);
            }
            final Object result = getDelegateMethod.invoke(repo);
            return (result instanceof JdbcRepository) ? (JdbcRepository) result : null;
        } catch (final NoSuchMethodException e) {
            return null;
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failedToGetJdbcRepository(e);
        }
    }
}
