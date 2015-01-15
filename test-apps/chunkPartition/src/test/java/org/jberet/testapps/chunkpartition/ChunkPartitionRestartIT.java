/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.chunkpartition;

import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.metric.MetricImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class ChunkPartitionRestartIT extends AbstractIT {
    /**
     * Restarts the job execution failed in {@link ChunkPartitionIT#complete2Fail1Partitions()}
     * @throws Exception
     */
    @Test
    public void restartFailedPartition() throws Exception {
        this.params.setProperty("writer.sleep.time", "0");
        restartAndWait(getOriginalJobExecutionId(ChunkPartitionIT.jobChunkPartitionFailComplete));

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        final List<PartitionExecutionImpl> partitionExecutions = stepExecution0.getPartitionExecutions();

        //1 partition should completed
        Assert.assertEquals(1, partitionExecutions.size());
        for (final PartitionExecutionImpl e : partitionExecutions) {
            final BatchStatus batchStatus = e.getBatchStatus();
            System.out.printf("Partition execution id: %s, status %s, StepExecution id: %s%n",
                    e.getPartitionId(), batchStatus, e.getStepExecutionId());
            Assert.assertEquals(BatchStatus.COMPLETED, e.getBatchStatus());
        }
        System.out.printf("StepExecution id: %s, metrics: %s%n", stepExecution0.getStepExecutionId(),
                java.util.Arrays.toString(stepExecution0.getMetrics()));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(3, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(7, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(7, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.FILTER_COUNT));
    }
}
