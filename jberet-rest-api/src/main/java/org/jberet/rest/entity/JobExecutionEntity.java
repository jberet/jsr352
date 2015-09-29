/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.batch.runtime.JobExecution;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public final class JobExecutionEntity extends AbstractExecutionEntity implements JobExecution, Serializable {
    private static final long serialVersionUID = -8566764098276314827L;

    private long executionId;
    private Date createTime;
    private Date lastUpdatedTime;
    private Properties jobParameters;
    private String jobName;

    private JobInstanceEntity jobInstance;
    private  List<StepExecutionEntity> stepExecutions = new ArrayList<StepExecutionEntity>();

    public JobExecutionEntity() {
    }

    public JobExecutionEntity(final JobExecution jobExecution) {
        super(jobExecution.getStartTime(), jobExecution.getEndTime(),
                jobExecution.getBatchStatus(), jobExecution.getExitStatus());
        executionId = jobExecution.getExecutionId();
        createTime = jobExecution.getCreateTime();
        lastUpdatedTime = jobExecution.getLastUpdatedTime();
        jobName = jobExecution.getJobName();
        jobParameters = jobExecution.getJobParameters();
    }

    public long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(long executionId) {
        this.executionId = executionId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public Properties getJobParameters() {
        return jobParameters;
    }

    public void setJobParameters(Properties jobParameters) {
        this.jobParameters = jobParameters;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @XmlTransient
    public JobInstanceEntity getJobInstance() {
        return jobInstance;
    }

    public void setJobInstance(JobInstanceEntity jobInstance) {
        this.jobInstance = jobInstance;
    }

    @XmlTransient
    public List<StepExecutionEntity> getStepExecutions() {
        return stepExecutions;
    }

    public void setStepExecutions(List<StepExecutionEntity> stepExecutions) {
        this.stepExecutions = stepExecutions;
    }
}
