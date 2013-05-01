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
import java.util.Properties;
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
     * To remember the partition properties for the failed or stopped partitions.  This field should not be cloned.
     * On the main StepExecution, a list of partition properties from failed or stopped partitions are added at the end
     * of a partitioned step execution; on a partition StepExecutionImpl clone, a resolved single partition properties
     * is added before starting the partition.
     */
    private List<Properties> partitionProperties;

    public StepExecutionImpl(long id, String stepName) {
        this.id = id;
        this.stepName = stepName;
        startTime = System.currentTimeMillis();
    }

    @Override
    public StepExecutionImpl clone() throws CloneNotSupportedException {
        StepExecutionImpl result = null;
        try {
            result = (StepExecutionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, "", stepName);
        }
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

    public List<Properties> getPartitionProperties() {
        return partitionProperties;
    }

    public void addPartitionProperties(Properties p) {
        if (partitionProperties == null) {
            partitionProperties = new ArrayList<Properties>();
        }
        partitionProperties.add(p);
    }
}
