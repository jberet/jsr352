/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.context;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Step;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Represents the execution context for either a step execution or partition execution.
 */
public class StepContextImpl extends AbstractContext implements StepContext, Cloneable {
    private Step step;

    /**
     * The step execution or partition execution associated with this context
     */
    private AbstractStepExecution stepExecution;

    private StepExecutionImpl originalStepExecution;
    Boolean allowStartIfComplete;

    public StepContextImpl(final Step step, final AbstractContext[] outerContexts) {
        super(step.getId(), outerContexts);
        this.step = step;
        this.classLoader = getJobContext().getClassLoader();
        this.stepExecution = getJobContext().jobRepository.createStepExecution(id);

        final JobExecutionImpl originalToRestart = getJobContext().originalToRestart;
        if (originalToRestart != null) {  //currently in a restarted execution
            originalStepExecution = getJobContext().getJobRepository().findOriginalStepExecutionForRestart(id, originalToRestart);
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
                //partition execution data from previous step execution will be carried over later in step runner, if needed.
            } else {
                BatchLogger.LOGGER.couldNotFindOriginalStepToRestart(stepExecution.getStepExecutionId(), getStepName());
            }
        }

        this.stepExecution.setBatchStatus(BatchStatus.STARTING);
    }

    @Override
    public StepContextImpl clone() {
        StepContextImpl c = null;
        try {
            c = (StepContextImpl) super.clone();
            c.stepExecution = new PartitionExecutionImpl(stepExecution);
            c.outerContexts = new AbstractContext[outerContexts.length];
            c.outerContexts[0] = getJobContext().clone();
            for (int i = 1; i < c.outerContexts.length; i++) {
                c.outerContexts[i] = outerContexts[i];
            }
            if (WildFlySecurityManager.isChecking()) {
                c.step = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Step>() {
                    @Override
                    public Step run() {
                        return BatchUtil.clone(step);
                    }
                });
            } else {
                c.step = BatchUtil.clone(step);
            }
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, getJobContext().getJobName(), getStepName());
        }
        return c;
    }

    public Step getStep() {
        return this.step;
    }

    public AbstractStepExecution getStepExecution() {
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
    public void setBatchStatus(final BatchStatus status) {
        stepExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return stepExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(final String exitStatus) {
        stepExecution.setExitStatus(exitStatus);
    }

    @Override
    public long getStepExecutionId() {
        return stepExecution.getStepExecutionId();
    }

    @Override
    public Properties getProperties() {
        return org.jberet.job.model.Properties.toJavaUtilProperties(step.getProperties());
    }

    @Override
    public Serializable getPersistentUserData() {
        return stepExecution.getPersistentUserData();
    }

    @Override
    public void setPersistentUserData(final Serializable data) {
        stepExecution.setPersistentUserData(data);
    }

    @Override
    public Exception getException() {
        return stepExecution.getException();
    }

    public void setException(final Exception e) {
        stepExecution.setException(e);
    }

    @Override
    public Metric[] getMetrics() {
        return stepExecution.getMetrics();
    }

    public void savePersistentData() {
        getJobContext().jobRepository.savePersistentData(getJobContext().jobExecution, stepExecution);
    }

    public StepExecutionImpl getOriginalStepExecution() {
        return originalStepExecution;
    }
}
