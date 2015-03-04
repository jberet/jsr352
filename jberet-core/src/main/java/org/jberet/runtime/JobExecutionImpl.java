/*
 * Copyright (c) 2012-2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet.creation.ArtifactCreationContext;
import org.jberet.job.model.Job;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

public final class JobExecutionImpl extends AbstractExecution implements JobExecution, Cloneable {
    private static final long serialVersionUID = 3706885354351337764L;

    private long id;

    private final JobInstanceImpl jobInstance;

    private Job substitutedJob;

    private final List<StepExecution> stepExecutions = new CopyOnWriteArrayList<StepExecution>();

    private final Properties jobParameters;

    protected long createTime;
    protected long lastUpdatedTime;

    /**
     * Which job-level step, flow, decision or split to restart this job execution, if it were to be restarted.
     */
    private String restartPosition;

    private transient CountDownLatch jobTerminationLatch = new CountDownLatch(1);
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private transient final List<JobStopNotificationListener> jobStopNotificationListeners = new ArrayList<JobStopNotificationListener>();

    public JobExecutionImpl(final JobInstanceImpl jobInstance, final Properties jobParameters) throws JobStartException {
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        if (WildFlySecurityManager.isChecking()) {
            this.substitutedJob = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Job>() {
                @Override
                public Job run() {
                    return BatchUtil.clone(jobInstance.unsubstitutedJob);
                }
            });
        } else {
            this.substitutedJob = BatchUtil.clone(jobInstance.unsubstitutedJob);
        }
        this.createTime = System.currentTimeMillis();
        setBatchStatus(BatchStatus.STARTING);
    }

    public JobExecutionImpl(final JobInstanceImpl jobInstance,
                            final long id,
                            final Properties jobParameters,
                            final Date createTime,
                            final Date startTime,
                            final Date endTime,
                            final Date lastUpdatedTime,
                            final String batchStatus,
                            final String exitStatus,
                            final String restartPosition) {
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        this.id = id;

        if (createTime != null) {
            this.createTime = createTime.getTime();
        }
        if (startTime != null) {
            this.startTime = startTime.getTime();
        }
        if (endTime != null) {
            this.endTime = endTime.getTime();
        }
        if (lastUpdatedTime != null) {
            this.lastUpdatedTime = lastUpdatedTime.getTime();
        }
        this.batchStatus = BatchStatus.valueOf(batchStatus);
        this.exitStatus = exitStatus;
        this.restartPosition = restartPosition;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public JobExecutionImpl clone() {
        JobExecutionImpl result = null;
        try {
            result = (JobExecutionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, getJobName(), "");
        }
        return result;
    }

    //It's possible the (fast) job is already terminated and the latch nulled when this method is called
    public void awaitTermination(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        if (jobTerminationLatch != null) {
            if (timeout <= 0) {
                jobTerminationLatch.await();
            } else {
                jobTerminationLatch.await(timeout, timeUnit);
            }
        }
    }

    public Job getSubstitutedJob() {
        return substitutedJob;
    }

    @Override
    public void setBatchStatus(final BatchStatus batchStatus) {
        super.setBatchStatus(batchStatus);
        lastUpdatedTime = System.currentTimeMillis();
    }

    @Override
    public long getExecutionId() {
        return id;
    }

    @Override
    public String getJobName() {
        return jobInstance.getJobName();
    }

    @Override
    public Date getCreateTime() {
        return new Date(createTime);
    }

    @Override
    public Date getLastUpdatedTime() {
        return new Date(lastUpdatedTime);
    }

    public JobInstanceImpl getJobInstance() {
        return jobInstance;
    }

    @Override
    public Properties getJobParameters() {
        return jobParameters;
    }

    public List<StepExecution> getStepExecutions() {
        return stepExecutions;
    }

    public void addStepExecution(final StepExecution stepExecution) {
        this.stepExecutions.add(stepExecution);
        lastUpdatedTime = System.currentTimeMillis();
    }

    public void setRestartPosition(final String restartPosition) {
        this.restartPosition = restartPosition;
    }

    public String getRestartPosition() {
        return restartPosition;
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public void stop() {
        stopRequested.set(true);
        final JobStopNotificationListener[] ls;
        synchronized (jobStopNotificationListeners) {
            ls = jobStopNotificationListeners.toArray(new JobStopNotificationListener[jobStopNotificationListeners.size()]);
        }
        for (final JobStopNotificationListener l : ls) {
            l.stopRequested();
        }
    }

    public void registerJobStopNotifier(final JobStopNotificationListener l) {
        synchronized (jobStopNotificationListeners) {
            jobStopNotificationListeners.add(l);
        }
    }

    public void unregisterJobStopNotifier(final JobStopNotificationListener l) {
        synchronized (jobStopNotificationListeners) {
            jobStopNotificationListeners.remove(l);
        }
    }

    public void setLastUpdatedTime(final long lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public void cleanUp() {
        substitutedJob = null;
        ArtifactCreationContext.removeCurrentArtifactCreationContext();
        jobTerminationLatch.countDown();
        jobTerminationLatch = null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final JobExecutionImpl that = (JobExecutionImpl) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
