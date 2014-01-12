/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

import java.io.Serializable;
import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.metric.StepMetrics;

public abstract class AbstractStepExecution extends AbstractExecution implements StepExecution {
    private static final long serialVersionUID = 1L;

    long id;

    String stepName;

    Serializable persistentUserData;

    Serializable readerCheckpointInfo;

    Serializable writerCheckpointInfo;

    Exception exception;

    StepMetrics stepMetrics = new StepMetrics();

    public abstract List<PartitionExecutionImpl> getPartitionExecutions();

    AbstractStepExecution() {
    }

    AbstractStepExecution(final String stepName) {
        this.stepName = stepName;
        startTime = System.currentTimeMillis();
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    protected AbstractStepExecution clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public long getStepExecutionId() {
        return this.id;
    }

    @Override
    public String getStepName() {
        return stepName;
    }

    @Override
    public Serializable getPersistentUserData() {
        return persistentUserData;
    }

    public void setPersistentUserData(final Serializable persistentUserData) {
        this.persistentUserData = persistentUserData;
    }

    @Override
    public Metric[] getMetrics() {
        return stepMetrics.getMetrics();
    }

    public StepMetrics getStepMetrics() {
        return this.stepMetrics;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(final Exception exception) {
        this.exception = exception;
    }

    @Override
    public void setBatchStatus(final BatchStatus batchStatus) {
        super.setBatchStatus(batchStatus);
        if (batchStatus == BatchStatus.COMPLETED ||
                batchStatus == BatchStatus.FAILED ||
                batchStatus == BatchStatus.STOPPED) {
            endTime = System.currentTimeMillis();
        }
    }

    public Serializable getReaderCheckpointInfo() {
        return readerCheckpointInfo;
    }

    public void setReaderCheckpointInfo(final Serializable readerCheckpointInfo) {
        this.readerCheckpointInfo = readerCheckpointInfo;
    }

    public Serializable getWriterCheckpointInfo() {
        return writerCheckpointInfo;
    }

    public void setWriterCheckpointInfo(final Serializable writerCheckpointInfo) {
        this.writerCheckpointInfo = writerCheckpointInfo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractStepExecution)) return false;

        final AbstractStepExecution that = (AbstractStepExecution) o;

        if (id != that.id) return false;
        if (!stepName.equals(that.stepName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + stepName.hashCode();
        return result;
    }
}
