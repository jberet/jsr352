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

    private String stepName;

    private Serializable persistentUserData;

    private Serializable readerCheckpointInfo;

    private Serializable writerCheckpointInfo;

    private Exception exception;

    private StepMetrics stepMetrics = new StepMetrics();

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

    public StepExecutionImpl(String stepName) {
        this.stepName = stepName;
        startTime = System.currentTimeMillis();
    }

    public void setId(long id) {
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

    public void setStartCount(int startCount) {
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

    public void setPersistentUserData(Serializable persistentUserData) {
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

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void setBatchStatus(BatchStatus batchStatus) {
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

    public void setReaderCheckpointInfo(Serializable readerCheckpointInfo) {
        this.readerCheckpointInfo = readerCheckpointInfo;
    }

    public Serializable getWriterCheckpointInfo() {
        return writerCheckpointInfo;
    }

    public void setWriterCheckpointInfo(Serializable writerCheckpointInfo) {
        this.writerCheckpointInfo = writerCheckpointInfo;
    }

    public int getNumOfPartitions() {
        return numOfPartitions;
    }

    public void setNumOfPartitions(int numOfPartitions) {
        this.numOfPartitions = numOfPartitions;
    }

    public List<Integer> getPartitionPropertiesIndex() {
        return partitionPropertiesIndex;
    }

    public void addPartitionPropertiesIndex(Integer i) {
        if (partitionPropertiesIndex == null) {
            partitionPropertiesIndex = new ArrayList<Integer>();
        }
        partitionPropertiesIndex.add(i);
    }

    public List<Serializable> getPartitionPersistentUserData() {
        return partitionPersistentUserData;
    }

    public void addPartitionPersistentUserData(Serializable d) {
        if (partitionPersistentUserData == null) {
            partitionPersistentUserData = new ArrayList<Serializable>();
        }
        partitionPersistentUserData.add(d);
    }

    public List<Serializable> getPartitionReaderCheckpointInfo() {
        return partitionReaderCheckpointInfo;
    }

    public void addPartitionReaderCheckpointInfo(Serializable s) {
        if (partitionReaderCheckpointInfo == null) {
            partitionReaderCheckpointInfo = new ArrayList<Serializable>();
        }
        partitionReaderCheckpointInfo.add(s);
    }

    public List<Serializable> getPartitionWriterCheckpointInfo() {
        return partitionWriterCheckpointInfo;
    }

    public void addPartitionWriterCheckpointInfo(Serializable s) {
        if (partitionWriterCheckpointInfo == null) {
            partitionWriterCheckpointInfo = new ArrayList<Serializable>();
        }
        partitionWriterCheckpointInfo.add(s);
    }
}
