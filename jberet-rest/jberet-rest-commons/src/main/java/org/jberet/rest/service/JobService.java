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

package org.jberet.rest.service;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.jberet.job.model.Job;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.operations.DelegatingJobOperator;
import org.jberet.rest.commons.util.JsonJobMapper;
import org.jberet.rest.entity.JobEntity;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.JobInstanceEntity;
import org.jberet.rest.entity.StepExecutionEntity;

/**
 * Facade class to {@code JobOperator} interface.
 *
 * @since 1.3.0
 */
public final class JobService {
    private static final JobService instance = new JobService();

    private final JobOperator jobOperator;

    private JobService() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    public static JobService getInstance() {
        return instance;
    }

    public JobExecutionEntity start(final String jobXmlName, final Properties jobParameters)
            throws JobStartException, JobSecurityException, NoSuchJobExecutionException {
        long jobExecutionId = jobOperator.start(jobXmlName, jobParameters);
        return new JobExecutionEntity(jobOperator.getJobExecution(jobExecutionId),
                jobOperator.getJobInstance(jobExecutionId).getInstanceId());
    }

    /**
     * Starts the job with the JSON job definition content.
     *
     * @param jobContent the content of the job definition in JSON format
     * @param jobParameters job parameters
     * @return the resultant job execution entity
     * @throws JobStartException
     * @throws JobSecurityException
     * @throws NoSuchJobExecutionException
     *
     * @since 1.3.0.Final
     */
    public JobExecutionEntity submit(final String jobContent, final Properties jobParameters)
            throws JobStartException, JobSecurityException, NoSuchJobExecutionException {
        AbstractJobOperator abstractJobOperator;
        if (jobOperator instanceof DelegatingJobOperator) {
            abstractJobOperator = ((AbstractJobOperator) ((DelegatingJobOperator) jobOperator).getDelegate());
        } else {
            abstractJobOperator = ((AbstractJobOperator) jobOperator);
        }
        final Job job = JsonJobMapper.toJob(jobContent);
        long jobExecutionId = abstractJobOperator.start(job, jobParameters);
        return new JobExecutionEntity(jobOperator.getJobExecution(jobExecutionId),
                jobOperator.getJobInstance(jobExecutionId).getInstanceId());
    }

    public JobEntity[] getJobs() throws JobSecurityException {
        final Set<String> jobNames = jobOperator.getJobNames();
        final JobEntity[] result = new JobEntity[jobNames.size()];
        int i = 0;
        for (final String jobName : jobNames) {
            final int jobInstanceCount = jobOperator.getJobInstanceCount(jobName);
            final List<Long> runningExecutions = jobOperator.getRunningExecutions(jobName);
            final JobEntity je = new JobEntity(jobName, jobInstanceCount, runningExecutions.size());
            result[i++] = je;
        }

        return result;
    }

    public JobInstanceEntity[] getJobInstances(final String jobName, final int start, final int count)
            throws NoSuchJobException, JobSecurityException {
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, start, count);
        final int len = jobInstances.size();

        final JobInstanceEntity[] jobInstanceData = new JobInstanceEntity[len];
        for (int i = 0; i < len; i++) {
            final JobInstance e = jobInstances.get(i);
            jobInstanceData[i] = new JobInstanceEntity(e, jobOperator.getJobExecutions(e));
        }
        return jobInstanceData;
    }

    public JobInstanceEntity getJobInstance(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobInstance jobInstance = jobOperator.getJobInstance(executionId);
        return new JobInstanceEntity(jobInstance, jobOperator.getJobExecutions(jobInstance));
    }

    public int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        return jobOperator.getJobInstanceCount(jobName);
    }

    public JobExecutionEntity getJobExecution(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionId);
        return new JobExecutionEntity(jobExecution, jobOperator.getJobInstance(jobExecutionId).getInstanceId());
    }

    public JobExecutionEntity[] getJobExecutions(int count, final long jobInstanceId, final long jobExecutionId1)
            throws NoSuchJobInstanceException, JobSecurityException {
        //pass null JobInstance to get ALL job executions
        JobInstance jobInstance = null;
        if (jobExecutionId1 > 0) {
            jobInstance = jobOperator.getJobInstance(jobExecutionId1);
        }

        final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstance);
        final int countAll = jobExecutions.size();
        if (count <= 0) {
            count = countAll;
        } else if (count > countAll) {
            count = countAll;
        }
        final JobExecutionEntity[] jobExecutionEntities = new JobExecutionEntity[count];
        for (int i = countAll - 1, j = 0; j < count && i >= 0; i--, j++) {
            final JobExecution e = jobExecutions.get(i);
            jobExecutionEntities[j] = new JobExecutionEntity(e,
                    jobOperator.getJobInstance(e.getExecutionId()).getInstanceId());
        }
        return jobExecutionEntities;
    }

    public void abandon(final long jobExecutionId)
            throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        jobOperator.abandon(jobExecutionId);
    }

    public void stop(final long jobExecutionId)
            throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        jobOperator.stop(jobExecutionId);
    }

    public JobExecutionEntity restart(final long jobExecutionId, final Properties restartParameters)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException,
            JobRestartException, JobSecurityException {
        final long restartExecutionId = jobOperator.restart(jobExecutionId, restartParameters);
        return new JobExecutionEntity(jobOperator.getJobExecution(restartExecutionId),
                jobOperator.getJobInstance(restartExecutionId).getInstanceId());
    }

    public JobExecutionEntity[] getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        final List<Long> executionIds = jobOperator.getRunningExecutions(jobName);
        final int len = executionIds.size();
        JobExecutionEntity[] runningExecutions = new JobExecutionEntity[len];

        for (int i = len - 1; i >= 0; i--) {
            final long e = executionIds.get(i);
            runningExecutions[len - 1 - i] = new JobExecutionEntity(jobOperator.getJobExecution(e),
                    jobOperator.getJobInstance(e).getInstanceId());
        }
        return runningExecutions;
    }

    public StepExecutionEntity[] getStepExecutions(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        final int len = stepExecutions.size();
        final StepExecutionEntity[] stepExecutionData = new StepExecutionEntity[len];

        for (int i = 0; i < len; i++) {
            stepExecutionData[i] = new StepExecutionEntity(stepExecutions.get(i));
        }
        return stepExecutionData;
    }

}
