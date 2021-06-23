/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Job;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;

public abstract class AbstractRepository implements JobRepository {
    final ConcurrentMap<ApplicationAndJobName, SoftReference<Job, ApplicationAndJobName>> jobs =
            new ConcurrentHashMap<ApplicationAndJobName, SoftReference<Job, ApplicationAndJobName>>();

    final ReferenceQueue<Job> jobReferenceQueue = new ReferenceQueue<Job>();

    abstract void insertJobInstance(JobInstanceImpl jobInstance);

    abstract void insertJobExecution(JobExecutionImpl jobExecution);

    abstract void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution);

    @Override
    public void addJob(final ApplicationAndJobName applicationAndJobName, final Job job) {
        //expunge stale entries
        for (Object x; (x = jobReferenceQueue.poll()) != null; ) {
            @SuppressWarnings("unchecked")
            final SoftReference<Job, ApplicationAndJobName> entry = (SoftReference<Job, ApplicationAndJobName>) x;
            jobs.remove(entry.getKey());
        }

        jobs.put(applicationAndJobName,
                new SoftReference<Job, ApplicationAndJobName>(job, jobReferenceQueue, applicationAndJobName));
    }

    @Override
    public Job getJob(final ApplicationAndJobName applicationAndJobName) {
        final SoftReference<Job, ApplicationAndJobName> jobSoftReference = jobs.get(applicationAndJobName);
        return jobSoftReference != null ? jobSoftReference.get() : null;
    }

    public boolean jobExists(final String jobName) {
        for (final ApplicationAndJobName e : jobs.keySet()) {
            if (e.jobName.equals(jobName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getJobNames() {
        final Set<String> jobNames = new HashSet<String>();
        for (final ApplicationAndJobName e : jobs.keySet()) {
            jobNames.add(e.jobName);
        }
        return jobNames;
    }


    @Override
    public void removeJob(final String jobId) {
        for (final Iterator<Map.Entry<ApplicationAndJobName, SoftReference<Job, ApplicationAndJobName>>> it =
             jobs.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<ApplicationAndJobName, SoftReference<Job, ApplicationAndJobName>> next = it.next();
            if (next.getKey().jobName.equals(jobId)) {
                BatchLogger.LOGGER.removing("Job", jobId);
                it.remove();
            }
        }
        //cascade delete to be performed by subclasses, which have access to jobInstances and jobExecutions data structure
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
    public void savePersistentData(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        // Does nothing as the serialized data is stored in serialized form and is immutable
    }

    @Override
    public int savePersistentDataIfNotStopping(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        return 1;
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate, final boolean saveJobParameters) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());
    }

    @Override
    public void stopJobExecution(final JobExecutionImpl jobExecution) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution, final PartitionExecutionImpl partitionExecution) {
        enclosingStepExecution.getPartitionExecutions().add(partitionExecution);
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
                                                               final StepExecutionImpl stepExecution,
                                                               final boolean notCompletedOnly,
                                                               final ClassLoader classLoader) {
        if (stepExecution != null) {
            final List<PartitionExecutionImpl> partitionExecutions = stepExecution.getPartitionExecutions();
            if (partitionExecutions == null) {
                return Collections.emptyList();
            }
            if (partitionExecutions.isEmpty() || !notCompletedOnly) {
                return partitionExecutions;
            }
            final List<PartitionExecutionImpl> result = new ArrayList<PartitionExecutionImpl>();
            for (final PartitionExecutionImpl sei : partitionExecutions) {
                if (sei.getBatchStatus() != BatchStatus.COMPLETED) {
                    result.add(sei);
                }
            }
            return result;
        }
        return null;
    }
}
