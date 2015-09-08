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
import java.util.List;
import javax.batch.runtime.BatchStatus;

public final class PartitionExecutionImpl extends AbstractStepExecution {
    private static final long serialVersionUID = 1L;

    /**
     * If this instance is assigned to a partition, partitionId represents the id for that partition.  Its value should
     * be the same as the partition attribute of the target partition properties in job xml.  The default value -1
     * indicates that this StepExecutionImpl is for the main step execution, and is not a cloned instance for any
     * partition.
     */
    private int partitionId = -1;

    /**
     * Creates a partition execution data structure.
     *
     * @param partitionId
     * @param stepExecutionId
     * @param stepName
     * @param batchStatus
     * @param exitStatus
     * @param persistentUserData
     * @param readerCheckpointInfo
     * @param writerCheckpointInfo
     */
    public PartitionExecutionImpl(final int partitionId,
                          final long stepExecutionId,
                          final String stepName,
                          final BatchStatus batchStatus,
                          final String exitStatus,
                          final Serializable persistentUserData,
                          final Serializable readerCheckpointInfo,
                          final Serializable writerCheckpointInfo
    ) {
        super(stepExecutionId, stepName, persistentUserData, readerCheckpointInfo, writerCheckpointInfo);
        this.partitionId = partitionId;
        this.batchStatus = batchStatus;
        this.exitStatus = exitStatus;
    }

    public PartitionExecutionImpl(final AbstractStepExecution stepExecution) {
        super(stepExecution);
        batchStatus = stepExecution.batchStatus;
        exitStatus = stepExecution.exitStatus;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(final int partitionId) {
        this.partitionId = partitionId;
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions() {
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof PartitionExecutionImpl)) return false;
        if (!super.equals(o)) return false;

        final PartitionExecutionImpl that = (PartitionExecutionImpl) o;

        if (partitionId != that.partitionId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + partitionId;
        return result;
    }
}
