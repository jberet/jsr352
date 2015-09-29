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

package org.jberet.rest.resource;

import java.util.ArrayList;
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
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.JobInstanceEntity;
import org.jberet.rest.entity.StepExecutionEntity;

final class JobService {
    private static final JobService instance = new JobService();

    private final JobOperator jobOperator;

    private JobService() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    static JobService getInstance() {
        return instance;
    }

    JobExecutionEntity start(final String jobXmlName, final Properties jobParameters)
            throws JobStartException, JobSecurityException, NoSuchJobExecutionException {
        long jobExecutionId = jobOperator.start(jobXmlName, jobParameters);
        return new JobExecutionEntity(jobOperator.getJobExecution(jobExecutionId));
    }

    Set<String> getJobNames() throws JobSecurityException {
        return jobOperator.getJobNames();
    }

    List<JobInstanceEntity> getJobInstances(final String jobName, final int start, final int count)
            throws NoSuchJobException, JobSecurityException {
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, start, count);
        final List<JobInstanceEntity> jobInstanceData = new ArrayList<JobInstanceEntity>();
        for (final JobInstance e : jobInstances) {
            jobInstanceData.add(new JobInstanceEntity(e));
        }
        return jobInstanceData;
    }

    public JobInstanceEntity getJobInstance(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobInstance jobInstance = jobOperator.getJobInstance(executionId);
        return new JobInstanceEntity(jobInstance);
    }

    int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        return jobOperator.getJobInstanceCount(jobName);
    }

    JobExecutionEntity getJobExecution(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionId);
        return new JobExecutionEntity(jobExecution);
    }

//    public List<JobExecution> getJobExecutions(final long jobInstanceId)
//            throws NoSuchJobInstanceException, JobSecurityException {
//        jobOperator.getInstan
//    }

    void abandon(final long jobExecutionId)
            throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        jobOperator.abandon(jobExecutionId);
    }

    void stop(final long jobExecutionId)
            throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        jobOperator.stop(jobExecutionId);
    }

    JobExecutionEntity restart(final long jobExecutionId, final Properties restartParameters)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException,
            JobRestartException, JobSecurityException {
        final long restartExecutionId = jobOperator.restart(jobExecutionId, restartParameters);
        return new JobExecutionEntity(jobOperator.getJobExecution(restartExecutionId));
    }

    List<JobExecutionEntity> getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        final List<Long> executionIds = jobOperator.getRunningExecutions(jobName);
        List<JobExecutionEntity> runningExecutions = new ArrayList<JobExecutionEntity>();
        for (final Long e : executionIds) {
            runningExecutions.add(new JobExecutionEntity(jobOperator.getJobExecution(e)));
        }
        return runningExecutions;
    }

    List<StepExecutionEntity> getStepExecutions(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        final List<StepExecutionEntity> stepExecutionData = new ArrayList<StepExecutionEntity>();
        for (final StepExecution e : stepExecutions) {
            stepExecutionData.add(new StepExecutionEntity(e));
        }
        return stepExecutionData;
    }

}
