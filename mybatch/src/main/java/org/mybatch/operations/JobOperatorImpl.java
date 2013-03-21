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
import java.util.HashMap;
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
    private ArtifactFactory artifactFactory = new SimpleArtifactFactory();
    private Map<Long, Future<?>> jobExecutionResults = new HashMap<Long, Future<?>>();

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        JobRepository repository = JobRepositoryFactory.getJobRepository();
        ClassLoader classLoader = BatchUtil.getBatchApplicationClassLoader();
        Job jobDefined = ArchiveXmlLoader.loadJobXml(jobXMLName, Job.class, classLoader);

        repository.addJob(jobDefined);
        ApplicationMetaData appData;
        try {
            appData = new ApplicationMetaData(classLoader);
        } catch (IOException e) {
            throw LOGGER.failToProcessMetaData(e, jobXMLName);
        }
        JobInstanceImpl instance = new JobInstanceImpl(repository.nextUniqueId(), jobDefined);
        JobExecutionImpl jobExecution = new JobExecutionImpl(repository.nextUniqueId(), instance, jobParameters);
        JobContextImpl jobContext = new JobContextImpl(jobDefined, jobExecution, appData, artifactFactory, repository);

        JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        Future<?> result = ConcurrencyService.submit(jobExecutionRunner);
        long jobExecutionId = jobExecution.getExecutionId();
        jobExecutionResults.put(jobExecutionId, result);

        repository.addJob(jobDefined);
        repository.addJobInstance(instance);
        repository.addJobExecution(jobExecution);

        return jobExecutionId;
    }

    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException,
            JobExecutionNotRunningException, JobSecurityException {
        Future<?> executionResult = jobExecutionResults.get(executionId);
        if (executionResult == null) {
            throw LOGGER.noSuchJobExecution(executionId);
        }
        //TODO check if need to throw JobExecutionNotRunningException

        //cancel the task if the task execution has not started
        executionResult.cancel(false);

        //TODO if the task execution has already started
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        return null;
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {
        return 0;
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count) throws NoSuchJobException, JobSecurityException {
        return null;
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {
        return null;
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return getJobExecution(executionId).getJobParameters();
    }

    @Override
    public long restart(long executionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        return 0;
    }

    @Override
    public void abandon(long executionId) throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {

    }

    @Override
    public JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution (executionId);
        return jobExecution.getJobInstance();
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance) throws NoSuchJobInstanceException, JobSecurityException {
        return ((JobInstanceImpl) instance).getJobExecutions();
    }

    @Override
    public JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return JobRepositoryFactory.getJobRepository().getJobExecution(executionId);
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution (jobExecutionId);
        return jobExecution.getStepExecutions();
    }
}
