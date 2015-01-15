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
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.metric.MetricImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class ChunkPartitionIT extends AbstractIT {
    static final String jobXml = "org.jberet.test.chunkPartition";
    static final String jobChunkPartitionFailComplete = "org.jberet.test.chunkPartitionFailComplete";
    static final String jobChunkPartitionMetricsCombined = "org.jberet.test.chunkPartitionMetricsCombined";

    @Test
    public void partitionThreads() throws Exception {
        for (int i = 10; i >= 8; i--) {
            params.setProperty("thread.count", String.valueOf(i));
            params.setProperty("writer.sleep.time", "100");
            startJobAndWait(jobXml);
            Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
            final String exitStatus = stepExecution0.getExitStatus();
            System.out.printf("Step exit status: %s%n", exitStatus);
            Assert.assertEquals(true, exitStatus.startsWith("PASS"));
        }

        params.setProperty("thread.count", "1");
        params.setProperty("skip.thread.check", "true");
        params.setProperty("writer.sleep.time", "0");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    @Test
    public void complete2Fail1Partitions() throws Exception {
        this.params = new Properties();
        this.params.setProperty("writer.sleep.time", "0");

        //at least 1 chunk (with item count 3) will be committed, so the subsequent restart will not start from scratch.
        this.params.setProperty("reader.fail.on.values", String.valueOf(-1));
        this.params.setProperty("writer.fail.on.values", String.valueOf(5));
        startJobAndWait(jobChunkPartitionFailComplete);

        //no skippable or retryable exceptions are configured, so this job execution will just fail
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        final List<PartitionExecutionImpl> partitionExecutions = stepExecution0.getPartitionExecutions();

        //2 should completed and 1 should failed, but the order can be random
        int completedPartitionCount = 0;
        int failedPartitionCount = 0;
        System.out.printf("StepExecution id: %s, step name: %s%n", stepExecution0.getStepExecutionId(), stepExecution0.getStepName());
        for (final PartitionExecutionImpl e : partitionExecutions) {
            final BatchStatus batchStatus = e.getBatchStatus();
            System.out.printf("Partition execution id: %s, status %s, StepExecution id: %s%n",
                    e.getPartitionId(), batchStatus, e.getStepExecutionId());
            if (batchStatus == BatchStatus.COMPLETED) {
                completedPartitionCount++;
            } else if (batchStatus == BatchStatus.FAILED) {
                failedPartitionCount++;
            } else {
                throw new RuntimeException("Unexpected partition execution batch status " + batchStatus);
            }
        }
        Assert.assertEquals(2, completedPartitionCount);
        Assert.assertEquals(1, failedPartitionCount);
        System.out.printf("StepExecution id: %s, metrics: %s%n", stepExecution0.getStepExecutionId(),
                java.util.Arrays.toString(stepExecution0.getMetrics()));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(9, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(26, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(23, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.FILTER_COUNT));
    }

    @Test
    /**
     * Verifies all metrics from all partitions are correctly consolidated into the metrics of the main step, and
     * any race conditions (all partitions trying to update the same metrics) are properly handled.
     */
    public void metricsCombined() throws Exception {
        this.params = new Properties();
        final int numOfPartitions = 20;
        startJobAndWait(jobChunkPartitionMetricsCombined);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        final List<PartitionExecutionImpl> partitionExecutions = stepExecution0.getPartitionExecutions();

        for (final PartitionExecutionImpl e : partitionExecutions) {
            final BatchStatus batchStatus = e.getBatchStatus();
            Assert.assertEquals(BatchStatus.COMPLETED, e.getBatchStatus());
        }
        Assert.assertEquals(numOfPartitions, partitionExecutions.size());

        System.out.printf("StepExecution id: %s, metrics: %s%n", stepExecution0.getStepExecutionId(),
                java.util.Arrays.toString(stepExecution0.getMetrics()));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(4 * numOfPartitions, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(200, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(200, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.FILTER_COUNT));
    }
}
