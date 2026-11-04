/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;

import org.jberet._private.BatchMessages;
import org.jberet.job.model.Job;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;

public interface JobRepository {
    void addJob(ApplicationAndJobName applicationAndJobName, Job job);

    void removeJob(String jobId);

    Job getJob(ApplicationAndJobName applicationAndJobName);

    /**
     * Gets all the job names from the job repository.
     * @return a set of job names
     *
     * @since 1.1.0.Final
     */
    Set<String> getJobNames();

    /**
     * Checks if a job with the specified {@code jobName} exists or not.
     * @param jobName the job name to check
     * @return true if the named job exists; false otherwise
     *
     * @since 1.1.0.Final
     */
    boolean jobExists(String jobName);

    JobInstanceImpl createJobInstance(Job job, String applicationName, ClassLoader classLoader);
    void removeJobInstance(long jobInstanceId);
    JobInstance getJobInstance(long jobInstanceId);
    List<JobInstance> getJobInstances(String jobName);
    int getJobInstanceCount(String jobName);

    JobExecutionImpl createJobExecution(JobInstanceImpl jobInstance, Properties jobParameters);
    JobExecution getJobExecution(long jobExecutionId);
    List<JobExecution> getJobExecutions(JobInstance jobInstance);

    /**
     * Gets job execution ids belonging to the job identified by the {@code jobName}.
     * @param jobName the job name identifying the job
     * @return job execution ids belonging to the job
     * @since 1.3.9.Final, 1.4.3.Final
     */
    List<Long> getJobExecutionsByJob(String jobName);

    /**
     * Gets job execution ids belonging to the job identified by the {@code jobName}.
     *
     * @param jobName the job name identifying the job
     * @param limit the maximum number of records that should be returned
     * @return job execution ids belonging to the job
     *
     * @since 1.3.11.Final, 1.4.4.Final
     */
    List<Long> getJobExecutionsByJob(String jobName, Integer limit);

    void updateJobExecution(JobExecutionImpl jobExecution, boolean fullUpdate, boolean saveJobParameters);

    /**
     * Updates the batch status to {@code STOPPING} in job repository,
     * including job execution, step execution, and partition execution status.
     *
     * @param jobExecution the job execution to be stopped
     * @since 1.3.8.Final
     */
    void stopJobExecution(JobExecutionImpl jobExecution);

    /**
     * Gets the ids of running job executions belonging to a specific job.
     *
     * @param jobName  the name of the job, not null
     * @return  a list of job execution ids
     *
     * @since 1.1.0.Final
     * @see org.jberet.operations.JobOperatorImpl#getRunningExecutions(java.lang.String)
     */
    List<Long> getRunningExecutions(final String jobName);

    /**
     * Removes JobExecutions based on the criteria specified in {@code jobExecutionSelector}.
     *
     * @param jobExecutionSelector criteria for which JobExecutions to remove
     *
     * @since 1.1.0.Beta1
     */
    void removeJobExecutions(JobExecutionSelector jobExecutionSelector);

    List<StepExecution> getStepExecutions(long jobExecutionId, ClassLoader classLoader);
    StepExecutionImpl createStepExecution(String stepName);
    void addStepExecution(JobExecutionImpl jobExecution, StepExecutionImpl stepExecution);
    void updateStepExecution(StepExecution stepExecution);
    StepExecutionImpl findOriginalStepExecutionForRestart(String stepName, JobExecutionImpl jobExecutionToRestart, ClassLoader classLoader);
    int countStepStartTimes(String stepName, long jobInstanceId);

    void addPartitionExecution(StepExecutionImpl enclosingStepExecution, PartitionExecutionImpl partitionExecution);
    List<PartitionExecutionImpl> getPartitionExecutions(long stepExecutionId, StepExecutionImpl stepExecution, boolean notCompletedOnly, ClassLoader classLoader);

    void savePersistentData(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution);

    /**
     * Saves the step or partition execution data to job repository if its
     * batch status is not {@code STOPPING}.
     *
     * @param jobExecution the current job execution
     * @param stepOrPartitionExecution the step or partition to save
     * @return 1 if saved successfully; 0 if not saved because its batch status is {@code STOPPING} in job repository
     *
     * @since 1.3.8.Final
     */
    int savePersistentDataIfNotStopping(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution);

    /**
     * Counts job executions of {@code jobName} in {@code STARTED} or {@code STOPPING} whose
     * effective last-updated time is strictly before {@code notUpdatedSince}. The effective
     * last-updated time is the last-updated time, falling back to the start time and then the
     * create time, so an execution that died before its first progress update is still counted.
     * <p>
     * This method is {@code default} and throws, so that existing third-party implementations of
     * this interface keep compiling and linking. Implementations that can support the
     * administrative operations on {@link org.jberet.operations.ExtendedJobOperator} override it.
     *
     * @param jobName         the job name; must not be {@code null}
     * @param notUpdatedSince the exclusive upper bound on the effective last-updated time,
     *                        computed from the caller's clock
     *
     * @return the number of matching job executions
     *
     * @throws UnsupportedOperationException if this repository cannot support the query
     * @since 2.2.0
     */
    default long countStaleExecutions(String jobName, Date notUpdatedSince) {
        throw BatchMessages.MESSAGES.adminOperationNotSupported(getClass().getName());
    }

    /**
     * Returns up to {@code count} job execution ids, skipping the first {@code start}, ordered by
     * ascending execution id, matching the same predicate as
     * {@link #countStaleExecutions(String, Date)}.
     *
     * @param jobName         the job name; must not be {@code null}
     * @param notUpdatedSince the exclusive upper bound on the effective last-updated time
     * @param start           zero-based offset of the first id to return; must be {@code >= 0}
     * @param count           maximum number of ids to return; must be {@code > 0}
     *
     * @return the matching execution ids in ascending order; never {@code null}, possibly empty
     *
     * @throws UnsupportedOperationException if this repository cannot support the query
     * @since 2.2.0
     */
    default List<Long> findStaleExecutionIds(String jobName, Date notUpdatedSince,
                                             int start, int count) {
        throw BatchMessages.MESSAGES.adminOperationNotSupported(getClass().getName());
    }

    /**
     * Fails the given executions and cascades to their non-terminal step and partition executions
     * in a single transaction.
     * <p>
     * Implementations must be all-or-nothing: either every eligible execution in
     * {@code executionIds} is transitioned, or none is. Executions that are not eligible are
     * reported through the returned map rather than aborting the operation.
     *
     * @param executionIds ids to attempt, already de-duplicated and capped by the caller
     * @param jobName      if non-null, an execution not belonging to this job yields
     *                     {@link FailOutcome.Kind#NOT_IN_SCOPE}
     * @param exitStatus   exit status to record on everything changed
     * @param now          timestamp for end time and last-updated time, from the caller's clock
     *
     * @return one outcome per requested id; never {@code null}
     *
     * @throws jakarta.batch.operations.BatchRuntimeException if the transaction could not be
     *         committed, in which case nothing was changed
     * @throws UnsupportedOperationException if this repository cannot support the operation
     * @since 2.2.0
     */
    default Map<Long, FailOutcome> failJobExecutions(Collection<Long> executionIds, String jobName,
                                                     String exitStatus, Date now) {
        throw BatchMessages.MESSAGES.adminOperationNotSupported(getClass().getName());
    }
}
