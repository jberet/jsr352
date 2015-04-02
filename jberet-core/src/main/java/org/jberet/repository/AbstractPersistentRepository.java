/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
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

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;

public abstract class AbstractPersistentRepository extends AbstractRepository implements JobRepository {
    final ConcurrentMap<Long, SoftReference<JobExecutionImpl>> jobExecutions = new ConcurrentHashMap<Long, SoftReference<JobExecutionImpl>>();
    final ConcurrentMap<Long, SoftReference<JobInstanceImpl>> jobInstances = new ConcurrentHashMap<Long, SoftReference<JobInstanceImpl>>();

    abstract void insertJobInstance(JobInstanceImpl jobInstance);

    abstract void insertJobExecution(JobExecutionImpl jobExecution);

    abstract void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution);

    abstract List<StepExecution> selectStepExecutions(final Long jobExecutionId, final ClassLoader classLoader);

    @Override
    public void removeJob(final String jobId) {
        super.removeJob(jobId);

        //perform cascade delete
        for (final Iterator<Map.Entry<Long, SoftReference<JobInstanceImpl>>> it = jobInstances.entrySet().iterator(); it.hasNext(); ) {
            final JobInstanceImpl ji = it.next().getValue().get();
            if (ji != null && ji.getJobName().equals(jobId)) {
                BatchLogger.LOGGER.removing(JobInstance.class.getName(), String.valueOf(ji.getInstanceId()));
                it.remove();
            }
        }

        for (final Iterator<Map.Entry<Long, SoftReference<JobExecutionImpl>>> it = jobExecutions.entrySet().iterator(); it.hasNext(); ) {
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
        for (final Iterator<Map.Entry<Long, SoftReference<JobExecutionImpl>>> it = jobExecutions.entrySet().iterator(); it.hasNext(); ) {
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
        final JobInstanceImpl jobInstance = new JobInstanceImpl(job, applicationName, job.getId());
        insertJobInstance(jobInstance);
        jobInstances.put(jobInstance.getInstanceId(), new SoftReference<JobInstanceImpl>(jobInstance));
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceIdToRemove) {
        BatchLogger.LOGGER.removing(JobInstance.class.getName(), String.valueOf(jobInstanceIdToRemove));
        jobInstances.remove(jobInstanceIdToRemove);
    }

    @Override
    public JobInstanceImpl getJobInstance(final long jobInstanceId) {
        final SoftReference<JobInstanceImpl> jobInstanceSoftReference = jobInstances.get(jobInstanceId);
        return jobInstanceSoftReference != null ? jobInstanceSoftReference.get() : null;
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        final JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        insertJobExecution(jobExecution);
        jobExecutions.put(jobExecution.getExecutionId(), new SoftReference<JobExecutionImpl>(jobExecution));
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        final SoftReference<JobExecutionImpl> jobExecutionSoftReference = jobExecutions.get(jobExecutionId);
        return jobExecutionSoftReference != null ? jobExecutionSoftReference.get() : null;
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        //check cache first, if not found, then retrieve from database
        final List<StepExecution> stepExecutions;
        final SoftReference<JobExecutionImpl> ref = jobExecutions.get(jobExecutionId);
        final JobExecution jobExecution = (ref != null) ? ref.get() : null;
        if (jobExecution == null) {
            stepExecutions = selectStepExecutions(jobExecutionId, classLoader);
        } else {
            final List<StepExecution> stepExecutions1 = ((JobExecutionImpl) jobExecution).getStepExecutions();
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
        StepExecutionImpl result = null;
        // the same-named StepExecution is not found in the jobExecutionToRestart.  It's still possible the same-named
        // StepExecution may exit in JobExecution earlier than jobExecutionToRestart for the same JobInstance.
        final long instanceId = jobExecutionToRestart.getJobInstance().getInstanceId();
        for (final SoftReference<JobExecutionImpl> e : jobExecutions.values()) {
            final JobExecutionImpl jobExecutionImpl = e.get();
            //skip the JobExecution that has already been checked above
            if (jobExecutionImpl != null && instanceId == jobExecutionImpl.getJobInstance().getInstanceId() &&
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
}
