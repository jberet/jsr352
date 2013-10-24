/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;

public abstract class AbstractRepository implements JobRepository {
    final ConcurrentMap<String, Job> jobs = new ConcurrentHashMap<String, Job>();
    final Map<Long, JobInstance> jobInstances = Collections.synchronizedMap(new LinkedHashMap<Long, JobInstance>());
    final ConcurrentMap<Long, JobExecution> jobExecutions = new ConcurrentHashMap<Long, JobExecution>();

    abstract void insertJobInstance(JobInstanceImpl jobInstance);
    abstract void insertJobExecution(JobExecutionImpl jobExecution);
    abstract void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution);

    @Override
    public void addJob(final Job job) {
        final Job existing = jobs.putIfAbsent(job.getId(), job);
        if (existing != null) {
            BatchLogger.LOGGER.jobAlreadyExists(job.getId());
        }
    }

    @Override
    public void removeJob(final String jobId) {
        jobs.remove(jobId);
        synchronized (jobInstances) {
            for (Iterator<Map.Entry<Long, JobInstance>> it = jobInstances.entrySet().iterator(); it.hasNext();) {
                final JobInstance ji = it.next().getValue();
                if (ji.getJobName().equals(jobId)) {
                    it.remove();
                }
            }
        }
        for (Iterator<Map.Entry<Long, JobExecution>> it = jobExecutions.entrySet().iterator(); it.hasNext();) {
            final JobExecution je = it.next().getValue();
            if (je.getJobName().equals(jobId)) {
                it.remove();
            }
        }
    }

    @Override
    public Job getJob(final String jobId) {
        return jobs.get(jobId);
    }

    @Override
    public Collection<Job> getJobs() {
        return jobs.values();
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        final ApplicationAndJobName appJobNames = new ApplicationAndJobName(applicationName, job.getId());
        final JobInstanceImpl jobInstance = new JobInstanceImpl(job, appJobNames);
        insertJobInstance(jobInstance);
        final JobInstance jobInstanceExisting = jobInstances.put(jobInstance.getInstanceId(), jobInstance);
        if (jobInstanceExisting != null) {
            throw BatchLogger.LOGGER.jobInstanceAlreadyExists(jobInstance.getInstanceId());
        }
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceIdToRemove) {
        jobInstances.remove(jobInstanceIdToRemove);
    }

    @Override
    public JobInstance getJobInstance(final long jobInstanceId) {
        return jobInstances.get(jobInstanceId);
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        final List<JobInstance> result = new ArrayList<JobInstance>();
        synchronized (jobInstances) {
            for (final JobInstance e : jobInstances.values()) {
                if (e.getJobName().equals(jobName)) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        int count = 0;
        synchronized (jobInstances) {
            for (final JobInstance e : jobInstances.values()) {
                if (e.getJobName().equals(jobName)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        final JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        insertJobExecution(jobExecution);
        final JobExecution jobExecutionExisting = jobExecutions.putIfAbsent(jobExecution.getExecutionId(), jobExecution);
        if (jobExecutionExisting != null) {
            throw BatchLogger.LOGGER.jobExecutionAlreadyExists(jobExecutionExisting.getExecutionId());
        }
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecution getJobExecution(final long jobExecutionId) {
        return jobExecutions.get(jobExecutionId);
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        if (jobInstance == null) {
            //return all JobExecution
            final List<JobExecution> result = new ArrayList<JobExecution>();
            result.addAll(this.jobExecutions.values());
            return result;
        } else {
            return ((JobInstanceImpl) jobInstance).getJobExecutions();
        }
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) {
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(jobExecutionId);
        return jobExecution.getStepExecutions();
    }

    @Override
    public StepExecutionImpl createStepExecution(final String stepName) {
//        this stepExecution will be added to jobExecution later, after determining restart-if-complete, so that
//        completed steps are not added to the enclosing JobExecution
//        jobExecution.addStepExecution(stepExecution);
        return new StepExecutionImpl(stepName);
    }

    @Override
    public void addStepExecution(final JobExecutionImpl jobExecution, final StepExecutionImpl stepExecution) {
        jobExecution.addStepExecution(stepExecution);
        insertStepExecution(stepExecution, jobExecution);
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution, final StepExecutionImpl stepExecution) {
        Serializable ser = stepExecution.getPersistentUserData();
        Serializable copy;
        if (ser != null) {
            copy = BatchUtil.clone(ser);
            stepExecution.setPersistentUserData(copy);
        }
        ser = stepExecution.getReaderCheckpointInfo();
        if (ser != null) {
            copy = BatchUtil.clone(ser);
            stepExecution.setReaderCheckpointInfo(copy);
        }
        ser = stepExecution.getWriterCheckpointInfo();
        if (ser != null) {
            copy = BatchUtil.clone(ser);
            stepExecution.setWriterCheckpointInfo(copy);
        }
        //save stepExecution partition properties
    }

    @Override
    public void updateJobExecution(final JobExecution jobExecution) {
        final JobExecutionImpl jobExecutionImpl = (JobExecutionImpl) jobExecution;
        jobExecutionImpl.setEndTime(System.currentTimeMillis());
    }
}
