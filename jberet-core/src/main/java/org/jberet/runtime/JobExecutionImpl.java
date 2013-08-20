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

import org.jberet.job.model.Job;
import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;

public final class JobExecutionImpl extends AbstractExecution implements JobExecution, Cloneable {
    public static final String JOB_EXECUTION_TIMEOUT_SECONDS_KEY = "org.jberet.job.execution.timeout.seconds";
    public static final long JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT = 300L;

    private long id;

    private final JobInstanceImpl jobInstance;

    private Job substitutedJob;

    private final List<StepExecution> stepExecutions = new ArrayList<StepExecution>();

    private final List<StepExecutionImpl> inactiveStepExecutions = new ArrayList<StepExecutionImpl>();

    private final Properties jobParameters;

    protected long createTime;
    protected long lastUpdatedTime;

    /**
     * Which job-level step, flow, decision or split to restart this job execution, if it were to be restarted.
     */
    String restartPoint;

    private CountDownLatch jobTerminationlatch = new CountDownLatch(1);
    private CountDownLatch jobStopLatch = new CountDownLatch(1);

    public JobExecutionImpl(final JobInstanceImpl jobInstance, final Properties jobParameters) throws JobStartException {
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        this.substitutedJob = BatchUtil.clone(jobInstance.unsubstitutedJob);
        this.startTime = this.createTime = System.currentTimeMillis();
        setBatchStatus(BatchStatus.STARTING);
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
    public void awaitTerminatioin(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        if (jobTerminationlatch != null) {
            jobTerminationlatch.await(timeout, timeUnit);
        }
    }

    //It's possible the (fast) job is already terminated and the latch nulled when this method is called
    public void awaitStop(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        if (jobStopLatch != null) {
            jobStopLatch.await(timeout, timeUnit);
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

    public List<StepExecutionImpl> getInactiveStepExecutions() {
        return inactiveStepExecutions;
    }

    public void setRestartPoint(final String restartPoint) {
        this.restartPoint = restartPoint;
    }

    public String getRestartPoint() {
        return restartPoint;
    }

    public boolean isStopRequested() {
        return jobStopLatch.getCount() == 0;
    }

    public void stop() {
        jobStopLatch.countDown();
    }

    public void cleanUp() {
        jobTerminationlatch.countDown();
        jobStopLatch.countDown();
        jobStopLatch = null;
        jobTerminationlatch = null;
        substitutedJob = null;
        endTime = System.currentTimeMillis();
    }
}
