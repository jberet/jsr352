/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import org.mybatch.creation.ArtifactFactory;
import org.mybatch.creation.SimpleArtifactFactory;
import org.mybatch.job.Job;
import org.mybatch.metadata.ApplicationMetaData;
import org.mybatch.metadata.ArchiveXmlLoader;
import org.mybatch.repository.JobRepository;
import org.mybatch.repository.JobRepositoryFactory;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.JobInstanceImpl;
import org.mybatch.runtime.context.JobContextImpl;
import org.mybatch.runtime.runner.JobExecutionRunner;
import org.mybatch.util.BatchUtil;
import org.mybatch.util.ConcurrencyService;

import static org.mybatch.util.BatchLogger.LOGGER;

public class JobOperatorImpl implements JobOperator {
    JobRepository repository = JobRepositoryFactory.getJobRepository();
    private ArtifactFactory artifactFactory = new SimpleArtifactFactory();
    private Map<Long, Future<?>> jobExecutionResults = new HashMap<Long, Future<?>>();

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        ClassLoader classLoader = BatchUtil.getBatchApplicationClassLoader();
        Job jobDefined = ArchiveXmlLoader.loadJobXml(jobXMLName, Job.class, classLoader);

        ApplicationMetaData appData;
        try {
            appData = new ApplicationMetaData(classLoader);
        } catch (IOException e) {
            throw LOGGER.failToProcessMetaData(e, jobXMLName);
        }
        JobInstanceImpl instance = new JobInstanceImpl(repository.nextUniqueId(), jobDefined, appData);
        repository.addJob(jobDefined);
        repository.addJobInstance(instance);

        return startJobExecution(instance, jobParameters);
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
        return count;
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count) throws NoSuchJobException, JobSecurityException {
        LinkedList<JobInstance> result = new LinkedList<JobInstance>();
        for (JobInstance e : repository.getJobInstances()) {
            if (e.getJobName().equals(jobName)) {
                if (result.size() < count) {
                    result.addFirst(e);
                } else {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {
        List<Long> result = new ArrayList<Long>();
        for (JobExecution e : repository.getJobExecutions()) {
            BatchStatus s = e.getBatchStatus();
            if (s == BatchStatus.STARTING || s == BatchStatus.STARTED) {
                result.add(e.getExecutionId());
            }
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
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        if (jobExecution == null) {
            throw LOGGER.noSuchJobExecution(executionId);
        }
        BatchStatus previousStatus = jobExecution.getBatchStatus();
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
                newExecutionId = startJobExecution(jobInstance, restartParameters);
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
        return JobRepositoryFactory.getJobRepository().getJobExecution(executionId);
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws
            NoSuchJobExecutionException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(jobExecutionId);
        return jobExecution.getStepExecutions();
    }

    private long startJobExecution(JobInstanceImpl jobInstance, Properties jobParameters) throws JobStartException, JobSecurityException {
        JobExecutionImpl jobExecution = new JobExecutionImpl(repository.nextUniqueId(), jobInstance, jobParameters);
        JobContextImpl jobContext = new JobContextImpl(jobExecution, artifactFactory, repository);

        JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        Future<?> result = ConcurrencyService.submit(jobExecutionRunner);
        long jobExecutionId = jobExecution.getExecutionId();

        repository.addJobExecution(jobExecution);
        return jobExecutionId;
    }
}
