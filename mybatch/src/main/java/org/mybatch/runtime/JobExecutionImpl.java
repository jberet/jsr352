/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.mybatch.runtime;

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

import org.mybatch.job.Job;
import org.mybatch.util.BatchUtil;

public final class JobExecutionImpl extends AbstractExecution implements JobExecution {
    public static final String JOB_EXECUTION_TIMEOUT_SECONDS_KEY = "org.mybatch.job.execution.timeout.seconds";
    public static final long JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT = 300L;

    private long id;

    private JobInstanceImpl jobInstance;

    private Job substitutedJob;

    private List<StepExecution> stepExecutions = new ArrayList<StepExecution>();

    private List<StepExecutionImpl> inactiveStepExecutions = new ArrayList<StepExecutionImpl>();

    private Properties jobParameters;

    protected long createTime;
    protected long lastUpdatedTime;

    /**
     * Which job-level step, flow, decision or split to restart this job execution, if it were to be restarted.
     */
    String restartPoint;

    private CountDownLatch latch = new CountDownLatch(1);

    public JobExecutionImpl(long id, JobInstanceImpl jobInstance, Properties jobParameters) throws JobStartException {
        this.id = id;
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        this.jobInstance.addJobExecution(this);
        this.substitutedJob = BatchUtil.cloneJob(jobInstance.unsubstitutedJob);
    }

    public void awaitTerminatioin(long timeout, TimeUnit timeUnit) throws InterruptedException {
        latch.await(timeout, timeUnit);
    }

    public Job getSubstitutedJob() {
        return substitutedJob;
    }

    public void setSubstitutedJob(Job j) {
        this.substitutedJob = j;
    }

    @Override
    public void setBatchStatus(BatchStatus batchStatus) {
        super.setBatchStatus(batchStatus);
        if (batchStatus != BatchStatus.STARTING &&
                batchStatus != BatchStatus.STARTED &&
                batchStatus != BatchStatus.STOPPING) {
            latch.countDown();
        }
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

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setLastUpdatedTime(long lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public JobInstanceImpl getJobInstance() {
        return jobInstance;
    }

    @Override
    public Properties getJobParameters() {
        return jobParameters;
    }

    public List<StepExecution> getStepExecutions() {
        return Collections.unmodifiableList(this.stepExecutions);
    }

    public void addStepExecution(StepExecution stepExecution) {
        this.stepExecutions.add(stepExecution);
    }

    public List<StepExecutionImpl> getInactiveStepExecutions() {
        return inactiveStepExecutions;
    }

    public void setRestartPoint(String restartPoint) {
        this.restartPoint = restartPoint;
    }

    public String getRestartPoint() {
        return restartPoint;
    }
}
