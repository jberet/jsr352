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
import javax.batch.operations.JobOperator;
import javax.batch.operations.exception.JobExecutionAlreadyCompleteException;
import javax.batch.operations.exception.JobExecutionIsRunningException;
import javax.batch.operations.exception.JobExecutionNotMostRecentException;
import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.operations.exception.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.mybatch.creation.ArtifactFactory;
import org.mybatch.creation.SimpleArtifactFactory;
import org.mybatch.job.Job;
import org.mybatch.metadata.ApplicationMetaData;
import org.mybatch.metadata.ArchiveXmlLoader;
import org.mybatch.metadata.JobMerger;
import org.mybatch.repository.JobRepository;
import org.mybatch.repository.impl.MemoryRepository;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.JobInstanceImpl;
import org.mybatch.runtime.context.JobContextImpl;
import org.mybatch.runtime.runner.JobExecutionRunner;
import org.mybatch.util.BatchUtil;
import org.mybatch.util.ConcurrencyService;

import static org.mybatch.util.BatchLogger.LOGGER;

public class JobOperatorImpl implements JobOperator {
    //TODO use factory
    private JobRepository repository = new MemoryRepository();
    private ArtifactFactory artifactFactory = new SimpleArtifactFactory();
    private Map<Long, Future<?>> jobExecutionResults = new HashMap<Long, Future<?>>();

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException {
        return 0;
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count) throws NoSuchJobException {
        return null;
    }

    @Override
    public List<JobExecution> getRunningExecutions(String jobName) throws NoSuchJobException {
        return null;
    }

    @Override
    public List<JobExecution> getExecutions(JobInstance instance) throws NoSuchJobInstanceException {
        return null;
    }

    @Override
    public Properties getParameters(JobInstance instance) throws NoSuchJobInstanceException {
        return null;
    }

    @Override
    public long start(String job, Properties jobParameters) throws JobStartException {
        ClassLoader classLoader = BatchUtil.getBatchApplicationClassLoader();
        Job jobDefined = ArchiveXmlLoader.loadJobXml(job, Job.class, classLoader);
        JobMerger jobMerger = new JobMerger(jobDefined);  //find any possible parents and merge them in
        jobMerger.merge();

        repository.addJob(jobDefined);
        ApplicationMetaData appData;
        try {
            appData = new ApplicationMetaData(classLoader);
        } catch (IOException e) {
            throw LOGGER.failToProcessMetaData(e, job);
        }
        JobInstanceImpl instance = new JobInstanceImpl(jobDefined);
        JobExecutionImpl jobExecution = new JobExecutionImpl(instance, jobParameters);
        JobContextImpl jobContext = new JobContextImpl(jobDefined, jobExecution, appData, artifactFactory);

        JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        Future<?> result = ConcurrencyService.submit(jobExecutionRunner);
        long jobExecutionId = jobExecution.getExecutionId();
        jobExecutionResults.put(jobExecutionId, result);
        return jobExecutionId;
    }

    @Override
    public long restart(long executionId)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException {
        return 0;
    }

    @Override
    public long restart(long executionId, Properties overrideJobParameters)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException {
        return 0;
    }

    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
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
    public void abandon(JobExecution jobExecution) throws NoSuchJobInstanceException, JobExecutionIsRunningException {

    }

    @Override
    public JobInstance getJobInstance(long instanceId) {
        return null;
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance) throws NoSuchJobInstanceException {
        return null;
    }

    @Override
    public JobExecution getJobExecution(long executionId) {
        return null;
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws NoSuchJobExecutionException {
        return null;
    }

    @Override
    public void purge(String apptag) {

    }

    @Override
    public Set<String> getJobNames() {
        return null;
    }

}
