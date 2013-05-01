/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012-2013, Red Hat, Inc., and individual contributors
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

package org.jberet.repository.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jberet.job.Job;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;

public enum MemoryRepository implements JobRepository {
    INSTANCE;

    private final AtomicLong atomicLong = new AtomicLong();

    private List<Job> jobs = Collections.synchronizedList(new ArrayList<Job>());

    private List<JobInstance> jobInstances = Collections.synchronizedList(new ArrayList<JobInstance>());

    private List<JobExecution> jobExecutions = Collections.synchronizedList(new ArrayList<JobExecution>());

    @Override
    public long nextUniqueId() {
        return atomicLong.incrementAndGet();
    }

    @Override
    public boolean addJob(Job job) {
        boolean result = false;
        synchronized (jobs) {
            if (!jobs.contains(job)) {
                result = jobs.add(job);
            }
        }
        return result;
    }

    @Override
    public boolean removeJob(String jobId) {
        synchronized (jobs) {
            Job toRemove = getJob(jobId);
            return toRemove == null ? false : jobs.remove(toRemove);
        }
    }

    @Override
    public Job getJob(String jobId) {
        synchronized (jobs) {
            for (Job j : jobs) {
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
    public boolean addJobExecution(JobExecution jobExecution) {
        return jobExecutions.add(jobExecution);
    }

    @Override
    public JobExecution getJobExecution(long jobExecutionId) {
        synchronized (jobExecutions) {
            for (JobExecution e : jobExecutions) {
                if (e.getExecutionId() == jobExecutionId) {
                    return e;
                }
            }
        }
        return null;
    }

    @Override
    public List<JobExecution> getJobExecutions() {
        return Collections.unmodifiableList(this.jobExecutions);
    }

    @Override
    public boolean addJobInstance(JobInstance jobInstance) {
        return jobInstances.add(jobInstance);
    }

    @Override
    public JobInstance getJobInstance(long jobInstanceId) {
        synchronized (jobInstances) {
            for (JobInstance e : jobInstances) {
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
    public void savePersistentData(JobExecution jobExecution, StepExecutionImpl stepExecution) {
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
