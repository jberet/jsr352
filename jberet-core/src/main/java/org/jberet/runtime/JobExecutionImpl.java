/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet.creation.ArtifactCreationContext;
import org.jberet.job.model.Job;
import org.jberet.util.BatchUtil;

public final class JobExecutionImpl extends AbstractExecution implements JobExecution, Cloneable {
    private static final long serialVersionUID = 3706885354351337764L;

    private long id;

    private final JobInstanceImpl jobInstance;

    private Job substitutedJob;

    private final List<StepExecution> stepExecutions = new ArrayList<StepExecution>();

    private final Properties jobParameters;

    protected long createTime;
    protected long lastUpdatedTime;

    /**
     * Which job-level step, flow, decision or split to restart this job execution, if it were to be restarted.
     */
    private String restartPosition;

    private transient CountDownLatch jobTerminationLatch = new CountDownLatch(1);
    private transient CountDownLatch jobStopLatch = new CountDownLatch(1);

    public JobExecutionImpl(final JobInstanceImpl jobInstance, final Properties jobParameters) throws JobStartException {
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        this.substitutedJob = BatchUtil.clone(jobInstance.unsubstitutedJob);
        this.startTime = this.createTime = System.currentTimeMillis();
        setBatchStatus(BatchStatus.STARTING);
    }

    public JobExecutionImpl(final JobInstanceImpl jobInstance,
                            final long id,
                            final Properties jobParameters,
                            final Timestamp createTime,
                            final Timestamp startTime,
                            final Timestamp endTime,
                            final Timestamp lastUpdatedTime,
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

    //It's possible the (fast) job is already terminated and the latch nulled when this method is called.
    public void awaitStop() throws InterruptedException {
        if (jobStopLatch != null) {
            jobStopLatch.await();
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
        synchronized (stepExecutions) {
            return Collections.unmodifiableList(stepExecutions);
        }
    }

    public void addStepExecution(final StepExecution stepExecution) {
        synchronized (stepExecutions) {
            this.stepExecutions.add(stepExecution);
        }
        lastUpdatedTime = System.currentTimeMillis();
    }

    public void setRestartPosition(final String restartPosition) {
        this.restartPosition = restartPosition;
    }

    public String getRestartPosition() {
        return restartPosition;
    }

    public boolean isStopRequested() {
        return jobStopLatch.getCount() == 0;
    }

    public void stop() {
        jobStopLatch.countDown();
    }

    public void setEndTime(final long endTime) {
        this.endTime = endTime;
        this.lastUpdatedTime = endTime;
    }

    public void cleanUp() {
        substitutedJob = null;
        ArtifactCreationContext.removeCurrentArtifactCreationContext();
        jobStopLatch.countDown();
        jobStopLatch = null;
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
