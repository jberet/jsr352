/*
 * Copyright (c) 2013-2018 Red Hat, Inc. and/or its affiliates.
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

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;

public abstract class AbstractPersistentRepository extends AbstractRepository implements JobRepository {
    final ConcurrentMap<Long, SoftReference<JobExecutionImpl, Long>> jobExecutions =
            new ConcurrentHashMap<Long, SoftReference<JobExecutionImpl, Long>>();

    final ConcurrentMap<Long, SoftReference<JobInstanceImpl, Long>> jobInstances =
            new ConcurrentHashMap<Long, SoftReference<JobInstanceImpl, Long>>();

    final ReferenceQueue<JobExecutionImpl> jobExecutionReferenceQueue = new ReferenceQueue<JobExecutionImpl>();
    final ReferenceQueue<JobInstanceImpl> jobInstanceReferenceQueue = new ReferenceQueue<JobInstanceImpl>();

    abstract void insertJobInstance(JobInstanceImpl jobInstance);

    abstract void insertJobExecution(JobExecutionImpl jobExecution);

    abstract void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution);

    abstract List<StepExecution> selectStepExecutions(final Long jobExecutionId, final ClassLoader classLoader);

    @Override
    public void removeJob(final String jobId) {
        super.removeJob(jobId);

        //perform cascade delete
        for (final Iterator<Map.Entry<Long, SoftReference<JobInstanceImpl, Long>>> it =
             jobInstances.entrySet().iterator(); it.hasNext(); ) {
            final JobInstanceImpl ji = it.next().getValue().get();
            if (ji != null && ji.getJobName().equals(jobId)) {
                BatchLogger.LOGGER.removing(JobInstance.class.getName(), String.valueOf(ji.getInstanceId()));
                it.remove();
            }
        }

        for (final Iterator<Map.Entry<Long, SoftReference<JobExecutionImpl, Long>>> it =
             jobExecutions.entrySet().iterator(); it.hasNext(); ) {
            final JobExecutionImpl je = it.next().getValue().get();
            if (je != null && je.getJobName().equals(jobId)) {
                if (je.getJobParameters() != null) {
                    je.getJobParameters().clear();
                }
                BatchLogger.LOGGER.removing(JobExecution.class.getName(), String.valueOf(je.getExecutionId()));
                it.remove();
            }
        }
    }

    @Override
    public void removeJobExecutions(final JobExecutionSelector jobExecutionSelector) {
        final Collection<Long> allJobExecutionIds = jobExecutions.keySet();
        for (final Iterator<Map.Entry<Long, SoftReference<JobExecutionImpl, Long>>> it =
             jobExecutions.entrySet().iterator(); it.hasNext(); ) {
            final JobExecutionImpl je = it.next().getValue().get();
            if (je != null &&
                    (jobExecutionSelector == null || jobExecutionSelector.select(je, allJobExecutionIds))) {
                if (je.getJobParameters() != null) {
                    je.getJobParameters().clear();
                }
                BatchLogger.LOGGER.removing(JobExecution.class.getName(), String.valueOf(je.getExecutionId()));
                it.remove();
            }
        }
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        //expunge stale entries
        for (Object x; (x = jobInstanceReferenceQueue.poll()) != null; ) {
            @SuppressWarnings("unchecked")
            final SoftReference<JobInstanceImpl, Long> entry = (SoftReference<JobInstanceImpl, Long>) x;
            jobInstances.remove(entry.getKey());
        }

        final JobInstanceImpl jobInstance = new JobInstanceImpl(job, applicationName, job.getId());
        insertJobInstance(jobInstance);
        final Long instanceId = jobInstance.getInstanceId();
        jobInstances.put(instanceId,
                new SoftReference<JobInstanceImpl, Long>(jobInstance, jobInstanceReferenceQueue, instanceId));
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceIdToRemove) {
        BatchLogger.LOGGER.removing(JobInstance.class.getName(), String.valueOf(jobInstanceIdToRemove));
        jobInstances.remove(jobInstanceIdToRemove);
    }

    @Override
    public JobInstanceImpl getJobInstance(final long jobInstanceId) {
        final SoftReference<JobInstanceImpl, Long> jobInstanceSoftReference = jobInstances.get(jobInstanceId);
        return jobInstanceSoftReference != null ? jobInstanceSoftReference.get() : null;
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        //expunge stale entries
        for (Object x; (x = jobExecutionReferenceQueue.poll()) != null; ) {
            @SuppressWarnings("unchecked")
            final SoftReference<JobExecutionImpl, Long> entry = (SoftReference<JobExecutionImpl, Long>) x;
            jobExecutions.remove(entry.getKey());
        }

        final JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        insertJobExecution(jobExecution);
        final Long executionId = jobExecution.getExecutionId();
        jobExecutions.put(executionId,
                new SoftReference<JobExecutionImpl, Long>(jobExecution, jobExecutionReferenceQueue, executionId));
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        final SoftReference<JobExecutionImpl, Long> jobExecutionSoftReference = jobExecutions.get(jobExecutionId);
        return jobExecutionSoftReference != null ? jobExecutionSoftReference.get() : null;
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        //check cache first, if not found, then retrieve from database
        final List<StepExecution> stepExecutions;
        final SoftReference<JobExecutionImpl, Long> ref = jobExecutions.get(jobExecutionId);
        final JobExecutionImpl jobExecution = (ref != null) ? ref.get() : null;
        if (jobExecution == null) {
            stepExecutions = selectStepExecutions(jobExecutionId, classLoader);
        } else {
            final List<StepExecution> stepExecutions1 = jobExecution.getStepExecutions();
            if (stepExecutions1.isEmpty()) {
                stepExecutions = selectStepExecutions(jobExecutionId, classLoader);
            } else {
                stepExecutions = stepExecutions1;
            }
        }
        return stepExecutions;
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart,
                                                                 final ClassLoader classLoader) {
        for (final StepExecution stepExecution : jobExecutionToRestart.getStepExecutions()) {
            if (stepName.equals(stepExecution.getStepName())) {
                return (StepExecutionImpl) stepExecution;
            }
        }
        return null;
    }

    /**
     * Gets the list of job execution ids from the cache.
     * The result may be used as a supplement to that from the persistence store.
     *
     * @param jobName the job name
     * @param runningExecutionsOnly if true, only running executions are returned
     * @return list of job execution ids
     */
    List<Long> getCachedJobExecutions(final String jobName, final boolean runningExecutionsOnly) {
        final List<Long> result = new ArrayList<Long>();

        for (final Map.Entry<Long, SoftReference<JobExecutionImpl, Long>> e : jobExecutions.entrySet()) {
            final JobExecutionImpl jobExecution = e.getValue().get();
            if (jobExecution != null) {
                if (jobExecution.getJobName().equals(jobName)) {
                    if (runningExecutionsOnly) {
                        final BatchStatus s = jobExecution.getBatchStatus();
                        if (s == BatchStatus.STARTING || s == BatchStatus.STARTED) {
                            result.add(e.getKey());
                        }
                    } else {
                        result.add(e.getKey());
                    }
                }
            }
        }
        return result;
    }
}
