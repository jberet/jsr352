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

import java.util.List;
import java.util.Properties;
import java.util.Set;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;

import org.jberet.job.model.Job;
import org.jberet.operations.DefaultJobOperatorImpl;
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

    List<JobExecution> getTimeoutJobExecutions(JobInstance jobInstance, Long timeoutSeconds);

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
     * @see DefaultJobOperatorImpl#getRunningExecutions(java.lang.String)
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
}
