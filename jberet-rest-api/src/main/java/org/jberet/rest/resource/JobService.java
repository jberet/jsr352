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

import org.jberet.rest.model.JobExecutionData;
import org.jberet.rest.model.JobInstanceData;
import org.jberet.rest.model.StepExecutionData;

final class JobService {
    private static final JobService instance = new JobService();

    private final JobOperator jobOperator;

    private JobService() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    static JobService getInstance() {
        return instance;
    }

    JobExecutionData start(final String jobXmlName, final Properties jobParameters)
            throws JobStartException, JobSecurityException, NoSuchJobExecutionException {
        long jobExecutionId = jobOperator.start(jobXmlName, jobParameters);
        return new JobExecutionData(jobOperator.getJobExecution(jobExecutionId));
    }

    Set<String> getJobNames() throws JobSecurityException {
        return jobOperator.getJobNames();
    }

    List<JobInstanceData> getJobInstances(final String jobName, final int start, final int count)
            throws NoSuchJobException, JobSecurityException {
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, start, count);
        final List<JobInstanceData> jobInstanceData = new ArrayList<JobInstanceData>();
        for (final JobInstance e : jobInstances) {
            jobInstanceData.add(new JobInstanceData(e));
        }
        return jobInstanceData;
    }

    public JobInstanceData getJobInstance(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobInstance jobInstance = jobOperator.getJobInstance(executionId);
        return new JobInstanceData(jobInstance);
    }

    int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        return jobOperator.getJobInstanceCount(jobName);
    }

    JobExecutionData getJobExecution(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionId);
        return new JobExecutionData(jobExecution);
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

    JobExecutionData restart(final long jobExecutionId, final Properties restartParameters)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException,
            JobRestartException, JobSecurityException {
        final long restartExecutionId = jobOperator.restart(jobExecutionId, restartParameters);
        return new JobExecutionData(jobOperator.getJobExecution(restartExecutionId));
    }

    List<JobExecutionData> getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        final List<Long> executionIds = jobOperator.getRunningExecutions(jobName);
        List<JobExecutionData> runningExecutions = new ArrayList<JobExecutionData>();
        for (final Long e : executionIds) {
            runningExecutions.add(new JobExecutionData(jobOperator.getJobExecution(e)));
        }
        return runningExecutions;
    }

    List<StepExecutionData> getStepExecutions(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        final List<StepExecutionData> stepExecutionData = new ArrayList<StepExecutionData>();
        for (final StepExecution e : stepExecutions) {
            stepExecutionData.add(new StepExecutionData(e));
        }
        return stepExecutionData;
    }

}
