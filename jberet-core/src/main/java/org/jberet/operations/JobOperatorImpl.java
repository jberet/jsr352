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

package org.jberet.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
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
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jberet.creation.ArtifactFactory;
import org.jberet.creation.SimpleArtifactFactory;
import org.jberet.job.model.Job;
import org.jberet.metadata.ArchiveXmlLoader;
import org.jberet.repository.JobRepository;
import org.jberet.repository.JobRepositoryFactory;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.runner.JobExecutionRunner;
import org.jberet.util.BatchUtil;
import org.jberet.util.ConcurrencyService;

import static org.jberet.util.BatchLogger.LOGGER;

public class JobOperatorImpl implements JobOperator {
    JobRepository repository = JobRepositoryFactory.getJobRepository();
    private ArtifactFactory artifactFactory = new SimpleArtifactFactory();

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        ClassLoader classLoader = BatchUtil.getBatchApplicationClassLoader();
        Job jobDefined = ArchiveXmlLoader.loadJobXml(jobXMLName, Job.class, classLoader);
        repository.addJob(jobDefined);
        JobInstanceImpl jobInstance = repository.createJobInstance(jobDefined, getApplicationName(), classLoader);
        return startJobExecution(jobInstance, jobParameters, null);
    }

    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException,
            JobExecutionNotRunningException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) repository.getJobExecution(executionId);
        if (jobExecution == null) {
            throw LOGGER.noSuchJobExecution(executionId);
        }
        BatchStatus s = jobExecution.getBatchStatus();
        if (s == BatchStatus.STOPPED || s == BatchStatus.FAILED || s == BatchStatus.ABANDONED ||
                s == BatchStatus.COMPLETED) {
            throw LOGGER.jobExecutionNotRunningException(executionId, s);
        } else if (s == BatchStatus.STOPPING) {
            //in process of stopping, do nothing
        } else {
            jobExecution.setBatchStatus(BatchStatus.STOPPING);
            jobExecution.stop();
        }
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        Set<String> result = new HashSet<String>();
        for (Job e : repository.getJobs()) {
            result.add(e.getId());
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {
        int count = 0;
        for (JobInstance e : repository.getJobInstances()) {
            if (e.getJobName().equals(jobName)) {
                count++;
            }
        }
        if (count == 0) {
            throw LOGGER.noSuchJobException(jobName);
        }
        return count;
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count) throws NoSuchJobException, JobSecurityException {
        LinkedList<JobInstance> result = new LinkedList<JobInstance>();
        int pos = 0;
        List<JobInstance> instances = repository.getJobInstances();
        for (int i = instances.size() - 1; i >= 0; i--) {
            JobInstance e = instances.get(i);
            if (e.getJobName().equals(jobName)) {
                if (pos >= start) {
                    if (result.size() < count) {
                        result.add(e);
                    } else {
                        break;
                    }
                }
                pos++;
            }
        }
        if (pos == 0) {
            throw LOGGER.noSuchJobException(jobName);
        }
        return result;
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {
        List<Long> result = new ArrayList<Long>();
        boolean jobExists = false;
        for (JobExecution e : repository.getJobExecutions()) {
            BatchStatus s = e.getBatchStatus();
            if (e.getJobName().equals(jobName)) {
                jobExists = true;
                if (s == BatchStatus.STARTING || s == BatchStatus.STARTED) {
                    result.add(e.getExecutionId());
                }
            }
        }
        if (!jobExists) {
            throw LOGGER.noSuchJobException(jobName);
        }
        return result;
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return getJobExecution(executionId).getJobParameters();
    }

    @Override
    public long restart(long executionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException,
            NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        long newExecutionId = 0;
        JobExecutionImpl originalToRestart = (JobExecutionImpl) getJobExecution(executionId);
        if (originalToRestart == null) {
            throw LOGGER.noSuchJobExecution(executionId);
        }
        BatchStatus previousStatus = originalToRestart.getBatchStatus();
        if (previousStatus == BatchStatus.COMPLETED) {
            throw LOGGER.jobExecutionAlreadyCompleteException(executionId);
        }
        if (previousStatus == BatchStatus.ABANDONED ||
                previousStatus == BatchStatus.STARTED ||
                previousStatus == BatchStatus.STARTING ||
                previousStatus == BatchStatus.STOPPING) {
            throw LOGGER.jobRestartException(executionId, previousStatus);
        }
        if (previousStatus == BatchStatus.FAILED ||
                previousStatus == BatchStatus.STOPPED) {
            JobInstanceImpl jobInstance = (JobInstanceImpl) getJobInstance(executionId);
            List<JobExecution> executions = jobInstance.getJobExecutions();
            JobExecution mostRecentExecution = executions.get(executions.size() - 1);
            if (executionId != mostRecentExecution.getExecutionId()) {
                throw LOGGER.jobExecutionNotMostRecentException(executionId, jobInstance.getInstanceId());
            }
            try {
                newExecutionId = startJobExecution(jobInstance, restartParameters, originalToRestart);
            } catch (Exception e) {
                throw new JobRestartException(e);
            }
        }
        return newExecutionId;
    }

    @Override
    public void abandon(long executionId) throws
            NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        if (jobExecution == null) {
            throw LOGGER.noSuchJobExecution(executionId);
        }
        BatchStatus batchStatus = jobExecution.getBatchStatus();
        if (batchStatus == BatchStatus.COMPLETED ||
                batchStatus == BatchStatus.FAILED ||
                batchStatus == BatchStatus.STOPPED ||
                batchStatus == BatchStatus.ABANDONED) {
            jobExecution.setBatchStatus(BatchStatus.ABANDONED);
        } else {
            throw LOGGER.jobExecutionIsRunningException(executionId);
        }
    }

    @Override
    public JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        return jobExecution.getJobInstance();
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance) throws
            NoSuchJobInstanceException, JobSecurityException {
        return ((JobInstanceImpl) instance).getJobExecutions();
    }

    @Override
    public JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return repository.getJobExecution(executionId);
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws
            NoSuchJobExecutionException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(jobExecutionId);
        return jobExecution.getStepExecutions();
    }

    private long startJobExecution(JobInstanceImpl jobInstance, Properties jobParameters, JobExecutionImpl originalToRestart) throws JobStartException, JobSecurityException {
        JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, jobParameters);
        JobContextImpl jobContext = new JobContextImpl(jobExecution, originalToRestart, artifactFactory, repository);

        JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        Future<?> result = ConcurrencyService.submit(jobExecutionRunner);
        long jobExecutionId = jobExecution.getExecutionId();
        return jobExecutionId;
    }

    private String getApplicationName() {
        try {
            return InitialContext.doLookup("java:app/AppName");
        } catch (NamingException e) {
            return null;
        }
    }
}
