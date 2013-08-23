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
import java.util.List;
import java.util.Properties;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;

public abstract class AbstractRepository implements JobRepository {
    final List<Job> jobs = Collections.synchronizedList(new ArrayList<Job>());
    final List<JobInstance> jobInstances = Collections.synchronizedList(new ArrayList<JobInstance>());

    abstract void insertJobInstance(JobInstanceImpl jobInstance);
    abstract void insertJobExecution(JobExecutionImpl jobExecution);
    abstract void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution);

    @Override
    public boolean addJob(final Job job) {
        boolean result = false;
        synchronized (jobs) {
            if (!jobs.contains(job)) {
                result = jobs.add(job);
            }
        }
        return result;
    }

    @Override
    public boolean removeJob(final String jobId) {
        synchronized (jobs) {
            final Job toRemove = getJob(jobId);
            return toRemove != null && jobs.remove(toRemove);
        }
    }

    @Override
    public Job getJob(final String jobId) {
        synchronized (jobs) {
            for (final Job j : jobs) {
                if (j.getId().equals(jobId)) {
                    return j;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<Job> getJobs() {
        return Collections.unmodifiableCollection(jobs);
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        final ApplicationAndJobName appJobNames = new ApplicationAndJobName(applicationName, job.getId());
        final JobInstanceImpl jobInstance = new JobInstanceImpl(job, appJobNames);
        insertJobInstance(jobInstance);
        jobInstances.add(jobInstance);
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceIdToRemove) {
        synchronized (jobInstances) {
            for (Iterator<JobInstance> it = jobInstances.iterator(); it.hasNext(); ) {
                final JobInstance next = it.next();
                if (next.getInstanceId() == jobInstanceIdToRemove) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public JobInstance getJobInstance(final long jobInstanceId) {
        synchronized (jobInstances) {
            for (final JobInstance e : jobInstances) {
                if (e.getInstanceId() == jobInstanceId) {
                    return e;
                }
            }
        }
        return null;
    }

    @Override
    public List<JobInstance> getJobInstances() {
        return Collections.unmodifiableList(jobInstances);
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        final JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        insertJobExecution(jobExecution);
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecution getJobExecution(final long jobExecutionId) {
        synchronized (jobInstances) {
            for (final JobInstance e : jobInstances) {
                final JobInstanceImpl in = (JobInstanceImpl) e;
                for (final JobExecution j : in.getJobExecutions()) {
                    if (j.getExecutionId() == jobExecutionId) {
                        return j;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<JobExecution> getJobExecutions() {
        final List<JobExecution> result = new ArrayList<JobExecution>();
        synchronized (jobInstances) {
            for (final JobInstance e : jobInstances) {
                final JobInstanceImpl in = (JobInstanceImpl) e;
                result.addAll(in.getJobExecutions());
            }
        }
        return result;
    }

    @Override
    public StepExecutionImpl createStepExecution(final String stepName) {
        final StepExecutionImpl stepExecution = new StepExecutionImpl(stepName);

//        this stepExecution will be added to jobExecution later, after determining restart-if-complete, so that
//        completed steps are not added to the enclosing JobExecution
//        jobExecution.addStepExecution(stepExecution);
        return stepExecution;
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
}
