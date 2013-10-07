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
import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.metric.StepMetrics;
import org.jberet.util.BatchLogger;

public final class StepExecutionImpl extends AbstractExecution implements StepExecution, Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private long id;

    private final String stepName;

    private Serializable persistentUserData;

    private Serializable readerCheckpointInfo;

    private Serializable writerCheckpointInfo;

    private Exception exception;

    private final StepMetrics stepMetrics = new StepMetrics();

    int startCount;

    /**
     * To remember the numOfPartitions for restart purpose.  This field should not be cloned.  This field is set on the
     * main StepExecution only.
     */
    private int numOfPartitions;

    /**
     * To remember the index of the partition plan properties for failed or stopped partition on the step main thread.
     * This field should not be cloned.  On a cloned StepExecutionImpl, it contains a single index of the partition's
     * plan properties index.
     */
    private List<Integer> partitionPropertiesIndex;

    /**
     * To remember the persistent user data from each failed or stopped partition on the step main thread.  The element
     * order must be same as partitionPropertiesIndex.  This field should not be cloned.  On a cloned StepExecutionImpl,
     * it should not be used.
     */
    private List<Serializable> partitionPersistentUserData;

    /**
     * To remember the reader checkpoint info from each failed or stopped partition on the step main thread.  The 
     * element order must be same as other partition-related list in this class.  This field should not be clone.  On a
     * cloned StepExecutionImpl, it should not be used.
     */
    private List<Serializable> partitionReaderCheckpointInfo;

    /**
     * To remember the writer checkpoint info from each failed or stopped partition on the step main thread.  The 
     * element order must be same as other partition-related list in this class.  This field should not be clone.  On a
     * cloned StepExecutionImpl, it should not be used.
     */
    private List<Serializable> partitionWriterCheckpointInfo;

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
                             final long startTime,
                             final long endTime,
                             final String batchStatus,
                             final String exitStatus,
                             final Object persistentUserData,
                             final long readCount,
                             final long writeCount,
                             final long commitCount,
                             final long rollbackCount,
                             final long readSkipCount,
                             final long processSkipCount,
                             final long filterCount,
                             final long writeSkipCount) {
        this.id = id;
        this.stepName = stepName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.batchStatus = Enum.valueOf(BatchStatus.class, batchStatus);
        this.exitStatus = exitStatus;
        this.persistentUserData = (Serializable) persistentUserData;
        stepMetrics.set(Metric.MetricType.READ_COUNT, readCount);
        stepMetrics.set(Metric.MetricType.WRITE_COUNT, writeCount);
        stepMetrics.set(Metric.MetricType.COMMIT_COUNT, commitCount);
        stepMetrics.set(Metric.MetricType.ROLLBACK_COUNT, rollbackCount);
        stepMetrics.set(Metric.MetricType.READ_SKIP_COUNT, readSkipCount);
        stepMetrics.set(Metric.MetricType.PROCESS_SKIP_COUNT, processSkipCount);
        stepMetrics.set(Metric.MetricType.FILTER_COUNT, filterCount);
        stepMetrics.set(Metric.MetricType.WRITE_SKIP_COUNT, writeSkipCount);
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
        result.numOfPartitions = 0;
        result.partitionPropertiesIndex = null;
        return result;
    }

    public int getStartCount() {
        return startCount;
    }

    public void incrementStartCount() {
        this.startCount++;
    }

    public void setStartCount(final int startCount) {
        this.startCount = startCount;
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

    public int getNumOfPartitions() {
        return numOfPartitions;
    }

    public void setNumOfPartitions(final int numOfPartitions) {
        this.numOfPartitions = numOfPartitions;
    }

    public List<Integer> getPartitionPropertiesIndex() {
        return partitionPropertiesIndex;
    }

    public void addPartitionPropertiesIndex(final Integer i) {
        if (partitionPropertiesIndex == null) {
            partitionPropertiesIndex = new ArrayList<Integer>();
        }
        partitionPropertiesIndex.add(i);
    }

    public List<Serializable> getPartitionPersistentUserData() {
        return partitionPersistentUserData;
    }

    public void addPartitionPersistentUserData(final Serializable d) {
        if (partitionPersistentUserData == null) {
            partitionPersistentUserData = new ArrayList<Serializable>();
        }
        partitionPersistentUserData.add(d);
    }

    public List<Serializable> getPartitionReaderCheckpointInfo() {
        return partitionReaderCheckpointInfo;
    }

    public void addPartitionReaderCheckpointInfo(final Serializable s) {
        if (partitionReaderCheckpointInfo == null) {
            partitionReaderCheckpointInfo = new ArrayList<Serializable>();
        }
        partitionReaderCheckpointInfo.add(s);
    }

    public List<Serializable> getPartitionWriterCheckpointInfo() {
        return partitionWriterCheckpointInfo;
    }

    public void addPartitionWriterCheckpointInfo(final Serializable s) {
        if (partitionWriterCheckpointInfo == null) {
            partitionWriterCheckpointInfo = new ArrayList<Serializable>();
        }
        partitionWriterCheckpointInfo.add(s);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final StepExecutionImpl that = (StepExecutionImpl) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
