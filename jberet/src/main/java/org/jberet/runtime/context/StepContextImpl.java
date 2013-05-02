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

package org.jberet.runtime.context;

import java.io.Serializable;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.StepContext;

import org.jberet.job.Step;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;

public class StepContextImpl extends AbstractContext implements StepContext, Cloneable {
    private Step step;
    private StepExecutionImpl stepExecution;
    Boolean allowStartIfComplete;

    public StepContextImpl(Step step, AbstractContext[] outerContexts) {
        super(step.getId(), outerContexts);
        this.step = step;
        this.classLoader = getJobContext().getClassLoader();
        this.stepExecution = new StepExecutionImpl(getJobContext().jobRepository.nextUniqueId(), id);

        JobExecutionImpl originalToRestart = getJobContext().originalToRestart;
        if (originalToRestart != null) {  //currently in a restarted execution
            StepExecutionImpl originalStepExecution = findOriginalStepExecution(originalToRestart);
            if (originalStepExecution != null) {
                if (originalStepExecution.getBatchStatus() == BatchStatus.COMPLETED) {
                    allowStartIfComplete = Boolean.valueOf(step.getAllowStartIfComplete());
                    if (allowStartIfComplete == Boolean.FALSE) {
                        setBatchStatus(BatchStatus.COMPLETED);
                        setExitStatus(originalStepExecution.getExitStatus());
                        return;
                    }
                }
                if (originalStepExecution.getPersistentUserData() != null) {
                    this.stepExecution.setPersistentUserData(originalStepExecution.getPersistentUserData());
                }
                this.stepExecution.setReaderCheckpointInfo(originalStepExecution.getReaderCheckpointInfo());
                this.stepExecution.setWriterCheckpointInfo(originalStepExecution.getWriterCheckpointInfo());
                this.stepExecution.setStartCount((originalStepExecution).getStartCount());
                this.stepExecution.setNumOfPartitions(originalStepExecution.getNumOfPartitions());
                if (originalStepExecution.getPartitionPropertiesIndex() != null) {
                    for (Integer i : originalStepExecution.getPartitionPropertiesIndex()) {
                        this.stepExecution.addPartitionPropertiesIndex(i);
                    }
                }
                if (originalStepExecution.getPartitionPersistentUserData() != null) {
                    for (Serializable d : originalStepExecution.getPartitionPersistentUserData()) {
                        this.stepExecution.addPartitionPersistentUserData(d);
                    }
                }
                if (originalStepExecution.getPartitionReaderCheckpointInfo() != null) {
                    for (Serializable d : originalStepExecution.getPartitionReaderCheckpointInfo()) {
                        this.stepExecution.addPartitionReaderCheckpointInfo(d);
                    }
                }
                if (originalStepExecution.getPartitionWriterCheckpointInfo() != null) {
                    for (Serializable d : originalStepExecution.getPartitionWriterCheckpointInfo()) {
                        this.stepExecution.addPartitionWriterCheckpointInfo(d);
                    }
                }
            }
        }

        this.stepExecution.setBatchStatus(BatchStatus.STARTING);
    }

    private StepExecutionImpl findOriginalStepExecution(JobExecutionImpl originalToRestart) {
        for (StepExecution s : originalToRestart.getStepExecutions()) {
            if (id.equals(s.getStepName())) {
                return (StepExecutionImpl) s;
            }
        }
        for (StepExecutionImpl s : originalToRestart.getInactiveStepExecutions()) {
            if (id.equals(s.getStepName())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public StepContextImpl clone() {
        StepContextImpl c = null;
        try {
            c = (StepContextImpl) super.clone();
            c.stepExecution = stepExecution.clone();
            c.outerContexts = new AbstractContext[outerContexts.length];
            c.outerContexts[0] = getJobContext().clone();
            for(int i = 1; i < c.outerContexts.length; i++) {
                c.outerContexts[i] = outerContexts[i];
            }
            c.step = BatchUtil.clone(step);
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, getJobContext().getJobName(), getStepName());
        }
        return c;
    }

    public Step getStep() {
        return this.step;
    }

    public StepExecutionImpl getStepExecution() {
        return this.stepExecution;
    }

    public Boolean getAllowStartIfComplete() {
        return allowStartIfComplete;
    }

    @Override
    public String getStepName() {
        return step.getId();
    }

    @Override
    public BatchStatus getBatchStatus() {
        return stepExecution.getBatchStatus();
    }

    @Override
    public void setBatchStatus(BatchStatus status) {
        stepExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return stepExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(String exitStatus) {
        stepExecution.setExitStatus(exitStatus);
    }

    @Override
    public long getStepExecutionId() {
        return stepExecution.getStepExecutionId();
    }

    @Override
    public Properties getProperties() {
        return BatchUtil.toJavaUtilProperties(step.getProperties());
    }

    @Override
    public org.jberet.job.Properties getProperties2() {
        return step.getProperties();
    }

    @Override
    public Serializable getPersistentUserData() {
        return stepExecution.getPersistentUserData();
    }

    @Override
    public void setPersistentUserData(Serializable data) {
        stepExecution.setPersistentUserData(data);
    }

    @Override
    public Exception getException() {
        return stepExecution.getException();
    }

    public void setException(Exception e) {
        stepExecution.setException(e);
    }

    @Override
    public Metric[] getMetrics() {
        return stepExecution.getMetrics();
    }

    public void savePersistentData() {
        getJobContext().jobRepository.savePersistentData(getJobContext().jobExecution, stepExecution);
    }
}
