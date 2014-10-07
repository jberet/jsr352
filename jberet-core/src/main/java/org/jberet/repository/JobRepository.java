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

import java.util.Collection;
import java.util.List;
import java.util.Properties;
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
    void addJob(Job job);

    void removeJob(String jobId);

    Job getJob(String jobId);

    Collection<Job> getJobs();

    JobInstanceImpl createJobInstance(Job job, String applicationName, ClassLoader classLoader);
    void removeJobInstance(long jobInstanceId);
    JobInstance getJobInstance(long jobInstanceId);
    List<JobInstance> getJobInstances(String jobName);
    int getJobInstanceCount(String jobName);

    JobExecutionImpl createJobExecution(JobInstanceImpl jobInstance, Properties jobParameters);
    JobExecution getJobExecution(long jobExecutionId);
    List<JobExecution> getJobExecutions(JobInstance jobInstance);
    void updateJobExecution(JobExecutionImpl jobExecution, boolean fullUpdate);

    List<StepExecution> getStepExecutions(long jobExecutionId);
    StepExecutionImpl createStepExecution(String stepName);
    void addStepExecution(JobExecutionImpl jobExecution, StepExecutionImpl stepExecution);
    void updateStepExecution(StepExecution stepExecution);
    StepExecutionImpl findOriginalStepExecutionForRestart(String stepName, JobExecutionImpl jobExecutionToRestart);
    int countStepStartTimes(String stepName, long jobInstanceId);

    void addPartitionExecution(StepExecutionImpl enclosingStepExecution, PartitionExecutionImpl partitionExecution);
    List<PartitionExecutionImpl> getPartitionExecutions(long stepExecutionId, StepExecutionImpl stepExecution, boolean notCompletedOnly);

    void savePersistentData(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution);

}
