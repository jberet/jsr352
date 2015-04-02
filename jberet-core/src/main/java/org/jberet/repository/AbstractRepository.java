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

import java.io.Serializable;
import java.security.AccessController;
import java.lang.ref.SoftReference;
import java.security.PrivilegedAction;
import java.util.ArrayList;
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
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

public abstract class AbstractRepository implements JobRepository {
    ConcurrentMap<ApplicationAndJobName, SoftReference<Job>> jobs = new ConcurrentHashMap<ApplicationAndJobName, SoftReference<Job>>();

    abstract void insertJobInstance(JobInstanceImpl jobInstance);

    abstract void insertJobExecution(JobExecutionImpl jobExecution);

    abstract void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution);

    @Override
    public void addJob(final ApplicationAndJobName applicationAndJobName, final Job job) {
        jobs.put(applicationAndJobName, new SoftReference<Job>(job));
    }

    @Override
    public Job getJob(final ApplicationAndJobName applicationAndJobName) {
        final SoftReference<Job> jobSoftReference = jobs.get(applicationAndJobName);
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
        for (final Iterator<Map.Entry<ApplicationAndJobName, SoftReference<Job>>> it = jobs.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<ApplicationAndJobName, SoftReference<Job>> next = it.next();
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
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate) {
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

    private static <T extends Serializable> T clone(final T object) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    return BatchUtil.clone(object);
                }
            });
        }
        return BatchUtil.clone(object);
    }
}
