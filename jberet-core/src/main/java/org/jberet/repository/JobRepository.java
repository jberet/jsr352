/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

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
    void updateJobExecution(JobExecutionImpl jobExecution, boolean fullUpdate, boolean saveJobParameters);

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

}
