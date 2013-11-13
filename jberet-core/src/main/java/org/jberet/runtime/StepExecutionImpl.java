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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet.runtime.metric.StepMetrics;

public final class StepExecutionImpl extends AbstractExecution implements StepExecution, Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private long id;

    /**
     * If this instance is assigned to a partition, partitionId represents the id for that partition.  Its value should
     * be the same as the partition attribute of the target partition properties in job xml.  The default value -1
     * indicates that this StepExecutionImpl is for the main step execution, and is not a cloned instance for any
     * partition.
     */
    private int partitionId = -1;

    private String stepName;

    private Serializable persistentUserData;

    private Serializable readerCheckpointInfo;

    private Serializable writerCheckpointInfo;

    private Exception exception;

    private StepMetrics stepMetrics = new StepMetrics();

    /**
     * For a partitioned step, records the partitions contained in the current step.  If it is a first-time started
     * step, it contains all partitions, which can be checked for restart purpose later.  For a restarted step, it
     * contains all FAILED or STOPPED partition executions from previous run of the same step in the same JobInstance.
     * These partition executions are carried over when the StepContext is created.  Note this field should only be
     * in the main step, and not in any StepExecution clones.
     */
    private List<StepExecutionImpl> partitionExecutions = new ArrayList<StepExecutionImpl>();

    public StepExecutionImpl(final String stepName) {
        this.stepName = stepName;
        startTime = System.currentTimeMillis();
    }

    /**
     * Creates StepExecutionImpl from database records.
     * @param id
     * @param stepName
     * @param startTime
     * @param endTime
     * @param batchStatus
     * @param exitStatus
     * @param persistentUserData
     * @param readCount
     * @param writeCount
     * @param commitCount
     * @param rollbackCount
     * @param readSkipCount
     * @param processSkipCount
     * @param filterCount
     * @param writeSkipCount
     */
    public StepExecutionImpl(final long id,
                             final String stepName,
                             final Timestamp startTime,
                             final Timestamp endTime,
                             final String batchStatus,
                             final String exitStatus,
                             final Serializable persistentUserData,
                             final long readCount,
                             final long writeCount,
                             final long commitCount,
                             final long rollbackCount,
                             final long readSkipCount,
                             final long processSkipCount,
                             final long filterCount,
                             final long writeSkipCount,
                             final Serializable readerCheckpointInfo,
                             final Serializable writerCheckpointInfo) {
        this.id = id;
        this.stepName = stepName;

        if (startTime != null) {
            this.startTime = startTime.getTime();
        }
        if (endTime != null) {
            this.endTime = endTime.getTime();
        }
        this.batchStatus = Enum.valueOf(BatchStatus.class, batchStatus);
        this.exitStatus = exitStatus;
        this.persistentUserData = persistentUserData;
        this.readerCheckpointInfo = readerCheckpointInfo;
        this.writerCheckpointInfo = writerCheckpointInfo;
        stepMetrics.set(Metric.MetricType.READ_COUNT, readCount);
        stepMetrics.set(Metric.MetricType.WRITE_COUNT, writeCount);
        stepMetrics.set(Metric.MetricType.COMMIT_COUNT, commitCount);
        stepMetrics.set(Metric.MetricType.ROLLBACK_COUNT, rollbackCount);
        stepMetrics.set(Metric.MetricType.READ_SKIP_COUNT, readSkipCount);
        stepMetrics.set(Metric.MetricType.PROCESS_SKIP_COUNT, processSkipCount);
        stepMetrics.set(Metric.MetricType.FILTER_COUNT, filterCount);
        stepMetrics.set(Metric.MetricType.WRITE_SKIP_COUNT, writeSkipCount);
    }

    /**
     * Creates a partition execution data structure.
     * @param partitionId
     * @param stepExecutionId
     * @param batchStatus
     * @param exitStatus
     * @param persistentUserData
     * @param readerCheckpointInfo
     * @param writerCheckpointInfo
     */
    public StepExecutionImpl(final int partitionId,
                             final long stepExecutionId,
                             final BatchStatus batchStatus,
                             final String exitStatus,
                             final Serializable persistentUserData,
                             final Serializable readerCheckpointInfo,
                             final Serializable writerCheckpointInfo
                             ) {
        this.partitionId = partitionId;
        this.id = stepExecutionId;
        this.batchStatus = batchStatus;
        this.exitStatus = exitStatus;
        this.persistentUserData = persistentUserData;
        this.readerCheckpointInfo = readerCheckpointInfo;
        this.writerCheckpointInfo = writerCheckpointInfo;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public StepExecutionImpl clone() throws CloneNotSupportedException {
        StepExecutionImpl result = null;
        try {
            result = (StepExecutionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, "", stepName);
        }
        result.partitionId = 0;     //overwrite the default -1 to indicate it's for a partition
        result.partitionExecutions = null;
        result.stepMetrics = new StepMetrics();
        return result;
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

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(final int partitionId) {
        this.partitionId = partitionId;
    }

    public List<StepExecutionImpl> getPartitionExecutions() {
        return partitionExecutions;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof StepExecutionImpl)) return false;

        final StepExecutionImpl that = (StepExecutionImpl) o;

        if (id != that.id) return false;
        if (partitionId != that.partitionId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + partitionId;
        return result;
    }
}
