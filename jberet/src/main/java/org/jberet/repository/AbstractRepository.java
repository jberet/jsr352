/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jberet.repository;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jberet.job.Job;
import org.jberet.metadata.ApplicationMetaData;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;

import static org.jberet.util.BatchLogger.LOGGER;

public abstract class AbstractRepository implements JobRepository {
    final List<Job> jobs = Collections.synchronizedList(new ArrayList<Job>());
    final List<JobInstance> jobInstances = Collections.synchronizedList(new ArrayList<JobInstance>());
    final ConcurrentMap<ApplicationAndJobName, ApplicationMetaData> applicationMetaDataMap = new ConcurrentHashMap<ApplicationAndJobName, ApplicationMetaData>();

    abstract long nextJobInstanceId();
    abstract long nextJobExecutionId();
    abstract long nextStepExecutionId();

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
            return toRemove != null && jobs.remove(toRemove);
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
    public JobInstanceImpl createJobInstance(Job job, String applicationName, ClassLoader classLoader) {
        ApplicationAndJobName appJobNames = new ApplicationAndJobName(applicationName, job.getId());
        JobInstanceImpl jobInstance = new JobInstanceImpl(nextJobInstanceId(), job, appJobNames);
        jobInstance.setApplicationMetaData(getApplicationMetaData(appJobNames, classLoader));
        jobInstances.add(jobInstance);
        return jobInstance;
    }

    @Override
    public void removeJobInstance(long jobInstanceIdToRemove) {
        synchronized (jobInstances) {
            for (Iterator<JobInstance> it = jobInstances.iterator(); it.hasNext(); ) {
                JobInstance next = it.next();
                if (next.getInstanceId() == jobInstanceIdToRemove) {
                    it.remove();
                }
            }
        }
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
    public JobExecutionImpl createJobExecution(JobInstanceImpl jobInstance, Properties jobParameters) {
        JobExecutionImpl jobExecution = new JobExecutionImpl(nextJobExecutionId(), jobInstance, jobParameters);
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecution getJobExecution(long jobExecutionId) {
        synchronized (jobInstances) {
            for (JobInstance e : jobInstances) {
                JobInstanceImpl in = (JobInstanceImpl) e;
                for (JobExecution j : in.getJobExecutions()) {
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
        List<JobExecution> result = new ArrayList<JobExecution>();
        synchronized (jobInstances) {
            for (JobInstance e : jobInstances) {
                JobInstanceImpl in = (JobInstanceImpl) e;
                result.addAll(in.getJobExecutions());
            }
        }
        return result;
    }

    @Override
    public StepExecutionImpl createStepExecution(String stepName) {
        StepExecutionImpl stepExecution = new StepExecutionImpl(nextStepExecutionId(), stepName);

//        this stepExecution will be added to jobExecution later, after determining restart-if-complete, so that
//        completed steps are not added to the enclosing JobExecution
//        jobExecution.addStepExecution(stepExecution);
        return stepExecution;
    }

    @Override
    public void addStepExecution(JobExecutionImpl jobExecution, StepExecutionImpl stepExecution) {
        jobExecution.addStepExecution(stepExecution);
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

    protected ApplicationMetaData getApplicationMetaData(ApplicationAndJobName applicationAndJobName, ClassLoader classLoader) {
        ApplicationMetaData result = applicationMetaDataMap.get(applicationAndJobName);
        if (result == null) {
            try {
                result = new ApplicationMetaData(classLoader);
                ApplicationMetaData old = applicationMetaDataMap.putIfAbsent(applicationAndJobName, result);
                if (old != null) {
                    result = old;
                }
            } catch (IOException e) {
                throw LOGGER.failToProcessMetaData(e, applicationAndJobName.toString());
            }
        }

        return result;
    }
}
