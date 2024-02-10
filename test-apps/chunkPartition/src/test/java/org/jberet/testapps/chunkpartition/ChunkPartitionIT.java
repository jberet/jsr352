/*
 * Copyright (c) 2013-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunkpartition;

import java.util.List;
import java.util.Properties;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.Metric;

import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.metric.MetricImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class ChunkPartitionIT extends AbstractIT {
    static final String jobXml = "org.jberet.test.chunkPartition";
    static final String jobChunkPartitionFailComplete = "org.jberet.test.chunkPartitionFailComplete";
    static final String jobChunkPartitionMetricsCombined = "org.jberet.test.chunkPartitionMetricsCombined";
    static final String jobChunkPartitionRestart2StepsMapper = "org.jberet.test.chunkPartitionRestart2StepsMapper";
    static final String jobChunkPartitionRestart2StepsMapperOverride = "org.jberet.test.chunkPartitionRestart2StepsMapperOverride";

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

    /**
     * Runs the job {@link #jobChunkPartitionRestart2StepsMapper},
     * which includes 2 partitioned steps with
     * partition mappers. The job execution should fail at step1, and hence step2 will not be executed.
     *
     * @throws Exception
     */
    @Test
    public void failPartition2StepsMapper() throws Exception {
        this.params = new Properties();
        this.params.setProperty("writer.sleep.time", "0");

        this.params.setProperty("reader.fail.on.values", String.valueOf(15));
        startJobAndWait(jobChunkPartitionRestart2StepsMapper);

        //no skippable or retryable exceptions are configured, so this job execution will just fail
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());

        //step1 failed, and step2 did not get to run
        assertEquals(1, jobExecution.getStepExecutions().size());
    }

    /**
     * Similar to {@link #failPartition2StepsMapper()}, except that in this test partition mapper override is set to true.
     *
     * @throws Exception
     */
    @Test
    public void failPartition2StepsMapperOverride() throws Exception {
        this.params = new Properties();
        this.params.setProperty("writer.sleep.time", "0");
        this.params.setProperty("override", String.valueOf(true));
        this.params.setProperty("reader.fail.on.values", String.valueOf(15));
        startJobAndWait(jobChunkPartitionRestart2StepsMapperOverride);

        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        assertEquals(1, jobExecution.getStepExecutions().size());
    }
}
