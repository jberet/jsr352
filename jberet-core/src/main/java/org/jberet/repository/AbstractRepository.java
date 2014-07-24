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
import java.security.PrivilegedAction;
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
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.job.model.Job;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

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
            throw BatchMessages.MESSAGES.jobInstanceAlreadyExists(jobInstance.getInstanceId());
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
            throw BatchMessages.MESSAGES.jobExecutionAlreadyExists(jobExecutionExisting.getExecutionId());
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
    public StepExecutionImpl createStepExecution(final String stepName) {
//        this stepExecution will be added to jobExecution later, after determining restart-if-complete, so that
//        completed steps are not added to the enclosing JobExecution
//        jobExecution.addStepExecution(stepExecution);
        return new StepExecutionImpl(stepName);
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) {
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(jobExecutionId);
        return jobExecution.getStepExecutions();
    }

    @Override
    public void addStepExecution(final JobExecutionImpl jobExecution, final StepExecutionImpl stepExecution) {
        jobExecution.addStepExecution(stepExecution);
        insertStepExecution(stepExecution, jobExecution);
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        //persistent data and checkpoint info can be mutable objects, so serialize them to avoid further modification.
        Serializable ser = stepOrPartitionExecution.getPersistentUserData();
        Serializable copy;
        if (ser != null) {
            copy = clone(ser);
            stepOrPartitionExecution.setPersistentUserData(copy);
        }
        ser = stepOrPartitionExecution.getReaderCheckpointInfo();
        if (ser != null) {
            copy = clone(ser);
            stepOrPartitionExecution.setReaderCheckpointInfo(copy);
        }
        ser = stepOrPartitionExecution.getWriterCheckpointInfo();
        if (ser != null) {
            copy = clone(ser);
            stepOrPartitionExecution.setWriterCheckpointInfo(copy);
        }
        //save stepExecution partition properties
    }

    @Override
    public void updateJobExecution(final JobExecution jobExecution) {
        final JobExecutionImpl jobExecutionImpl = (JobExecutionImpl) jobExecution;
        jobExecutionImpl.setEndTime(System.currentTimeMillis());
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName, final JobExecutionImpl jobExecutionToRestart) {
        for (final StepExecution stepExecution : jobExecutionToRestart.getStepExecutions()) {
            if (stepName.equals(stepExecution.getStepName())) {
                return (StepExecutionImpl) stepExecution;
            }
        }
        StepExecutionImpl result = null;
        // the same-named StepExecution is not found in the jobExecutionToRestart.  It's still possible the same-named
        // StepExecution may exit in JobExecution earlier than jobExecutionToRestart for the same JobInstance.
        final long instanceId = jobExecutionToRestart.getJobInstance().getInstanceId();
        for (final JobExecution jobExecution : jobExecutions.values()) {
            final JobExecutionImpl jobExecutionImpl = (JobExecutionImpl) jobExecution;
            //skip the JobExecution that has already been checked above
            if (instanceId == jobExecutionImpl.getJobInstance().getInstanceId() &&
                    jobExecutionImpl.getExecutionId() != jobExecutionToRestart.getExecutionId()) {
                for (final StepExecution stepExecution : jobExecutionImpl.getStepExecutions()) {
                    if (stepExecution.getStepName().equals(stepName)) {
                        if (result == null || result.getStepExecutionId() < stepExecution.getStepExecutionId()) {
                            result = (StepExecutionImpl) stepExecution;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution, final PartitionExecutionImpl partitionExecution) {
        enclosingStepExecution.getPartitionExecutions().add(partitionExecution);
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
                                                               final StepExecutionImpl stepExecution,
                                                               final boolean notCompletedOnly) {
        if (stepExecution != null) {
            final List<PartitionExecutionImpl> partitionExecutions = stepExecution.getPartitionExecutions();
            if (partitionExecutions.isEmpty() || !notCompletedOnly) {
                return partitionExecutions;
            }
            final List<PartitionExecutionImpl> result = new ArrayList<PartitionExecutionImpl>();
            for (PartitionExecutionImpl sei : partitionExecutions) {
                if (sei.getBatchStatus() != BatchStatus.COMPLETED) {
                    result.add(sei);
                }
            }
            return result;
        }
        return null;
    }

    private static <T extends Serializable> T clone(final T object) {
        if (WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    return BatchUtil.clone(object);
                }
            });
        }
        return BatchUtil.clone(object);
    }
}
