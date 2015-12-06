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

    private long id;

    private String stepName;

    private SerializableData persistentUserData;

    private SerializableData readerCheckpointInfo;

    private SerializableData writerCheckpointInfo;

    private SerializableData exception;

    StepMetrics stepMetrics = new StepMetrics();

    public abstract List<PartitionExecutionImpl> getPartitionExecutions();

    AbstractStepExecution() {
    }

    AbstractStepExecution(final String stepName) {
        this.stepName = stepName;
        startTime = System.currentTimeMillis();
    }

    AbstractStepExecution(final long id, final String stepName, final Serializable persistentUserData, final Serializable readerCheckpointInfo, final Serializable writerCheckpointInfo) {
        this.id = id;
        this.stepName = stepName;
        this.persistentUserData = SerializableData.of(persistentUserData);
        this.readerCheckpointInfo = SerializableData.of(readerCheckpointInfo);
        this.writerCheckpointInfo = SerializableData.of(writerCheckpointInfo);
    }

    AbstractStepExecution(final AbstractStepExecution step) {
        this(step.id, step.stepName, step.persistentUserData, step.readerCheckpointInfo, step.writerCheckpointInfo);
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
        return deserialize(persistentUserData);
    }

    public byte[] getPersistentUserDataSerialized() {
        return persistentUserData == null ? null : persistentUserData.getSerialized();
    }

    public void setPersistentUserData(final Serializable persistentUserData) {
        this.persistentUserData = SerializableData.of(persistentUserData);
    }

    @Override
    public Metric[] getMetrics() {
        return stepMetrics.getMetrics();
    }

    public StepMetrics getStepMetrics() {
        return this.stepMetrics;
    }

    public Exception getException() {
        return (Exception) deserialize(exception);
    }

    public void setException(final Exception exception) {
        this.exception = SerializableData.of(exception);
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
        return deserialize(readerCheckpointInfo);
    }

    public byte[] getReaderCheckpointInfoSerialized() {
        return readerCheckpointInfo == null ? null : readerCheckpointInfo.getSerialized();
    }

    public void setReaderCheckpointInfo(final Serializable readerCheckpointInfo) {
        this.readerCheckpointInfo = SerializableData.of(readerCheckpointInfo);
    }

    public Serializable getWriterCheckpointInfo() {
        return deserialize(writerCheckpointInfo);
    }

    public byte[] getWriterCheckpointInfoSerialized() {
        return writerCheckpointInfo == null ? null : writerCheckpointInfo.getSerialized();
    }

    public void setWriterCheckpointInfo(final Serializable writerCheckpointInfo) {
        this.writerCheckpointInfo = SerializableData.of(writerCheckpointInfo);
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

    private static Serializable deserialize(final SerializableData data) {
        if (data == null){
            return null;
        }
        return data.deserialize();
    }
}
