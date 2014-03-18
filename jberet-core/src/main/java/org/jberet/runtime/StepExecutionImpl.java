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
import java.util.Date;
import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

public final class StepExecutionImpl extends AbstractStepExecution {
    private static final long serialVersionUID = 1L;

    /**
     * For a partitioned step, records the partitions contained in the current step.  If it is a first-time started
     * step, it contains all partitions, which can be checked for restart purpose later.  For a restarted step, it
     * contains all FAILED or STOPPED partition executions from previous run of the same step in the same JobInstance.
     * These partition executions are carried over when the StepContext is created.  Note this field should only be
     * in the main step, and not in any StepExecution clones.
     */
    private List<PartitionExecutionImpl> partitionExecutions = new ArrayList<PartitionExecutionImpl>();

    public StepExecutionImpl(final String stepName) {
        super(stepName);
    }

    /**
     * Creates StepExecutionImpl from database records.
     *
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
                             final Date startTime,
                             final Date endTime,
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

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions() {
        return partitionExecutions;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final StepExecutionImpl that = (StepExecutionImpl) o;

        if (!partitionExecutions.equals(that.partitionExecutions)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + partitionExecutions.hashCode();
        return result;
    }
}
