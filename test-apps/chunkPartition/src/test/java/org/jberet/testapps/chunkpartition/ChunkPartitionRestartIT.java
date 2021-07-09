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
import jakarta.batch.runtime.StepExecution;

import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.metric.MetricImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.junit.Assert.assertEquals;

/**
 * Tests in this class restart failed job executions in {@link ChunkPartitionIT}.
 */
public class ChunkPartitionRestartIT extends AbstractIT {
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(final Description description) {
            System.out.printf("%nStarting test: %s%n%n", description.getMethodName());
        }
    };

    /**
     * Restarts the job execution failed in {@link ChunkPartitionIT#complete2Fail1Partitions()}.
     * The 2nd step (step2) did not get to run during the previous failed execution, and it should
     * execute successfully during the restart.
     *
     * @throws Exception
     */
    @Test
    public void restartFailedPartition() throws Exception {
        params = new Properties();
        params.setProperty("writer.sleep.time", "0");
        //params.setProperty("reader.fail.on.values", String.valueOf(-1));
        params.setProperty("writer.fail.on.values", String.valueOf(-1));

        restartAndWait(getOriginalJobExecutionId(ChunkPartitionIT.jobChunkPartitionFailComplete));

        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        final List<PartitionExecutionImpl> partitionExecutions = stepExecution0.getPartitionExecutions();

        //1 partition should completed.  The other 2 partitions had already completed during the original execution.
        assertEquals(1, partitionExecutions.size());
        for (final PartitionExecutionImpl e : partitionExecutions) {
            final BatchStatus batchStatus = e.getBatchStatus();
            System.out.printf("Partition execution id: %s, status %s, StepExecution id: %s%n",
                    e.getPartitionId(), batchStatus, e.getStepExecutionId());
            assertEquals(BatchStatus.COMPLETED, e.getBatchStatus());
        }
        System.out.printf("StepExecution id: %s, metrics: %s%n", stepExecution0.getStepExecutionId(),
                java.util.Arrays.toString(stepExecution0.getMetrics()));
        final StepExecution stepExecution2 = stepExecutions.get(1);
        System.out.printf("StepExecution id: %s, metrics: %s%n", stepExecution2.getStepExecutionId(),
                java.util.Arrays.toString(stepExecution2.getMetrics()));

        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.ROLLBACK_COUNT));
        assertEquals(3, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
        assertEquals(7, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        assertEquals(7, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.PROCESS_SKIP_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_SKIP_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_SKIP_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Restarts a failed job execution with 2 steps and partition mapper (partition mapper override = false).
     * In the original failed job execution, step1 failed, and step2 didn't get to execute. The restart
     * should start from step1 and continue to step2 successfully.
     *
     * @throws Exception
     *
     * @see ChunkPartitionIT#failPartition2StepsMapper()
     */
    @Test
    public void restartFailedPartition2StepsMapper() throws Exception {
        params = new Properties();
        params.setProperty("reader.fail.on.values", String.valueOf(-1));
        restartAndWait(getOriginalJobExecutionId(ChunkPartitionIT.jobChunkPartitionRestart2StepsMapper));

        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());

        //step1 should rerun successfully, and continue to run step2 successfully
        assertEquals(2, jobExecution.getStepExecutions().size());
    }

    /**
     * Similar to {@link #restartFailedPartition2StepsMapper()}, except that partition mapper override is set to true
     * in this test.
     *
     * @throws Exception
     */
    @Test
    public void restartFailedPartition2StepsMapperOverride() throws Exception {
        params = new Properties();
        params.setProperty("reader.fail.on.values", String.valueOf(-1));
        restartAndWait(getOriginalJobExecutionId(ChunkPartitionIT.jobChunkPartitionRestart2StepsMapperOverride));

        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        assertEquals(2, jobExecution.getStepExecutions().size());

        final List<PartitionExecutionImpl> partitionExecutions = stepExecution0.getPartitionExecutions();

        //all 3 partitions should be re-executed
        assertEquals(3, partitionExecutions.size());
        for (final PartitionExecutionImpl e : partitionExecutions) {
            final BatchStatus batchStatus = e.getBatchStatus();
            System.out.printf("Partition execution id: %s, status %s, StepExecution id: %s%n",
                    e.getPartitionId(), batchStatus, e.getStepExecutionId());
            assertEquals(BatchStatus.COMPLETED, e.getBatchStatus());
        }
        System.out.printf("StepExecution id: %s, metrics: %s%n", stepExecution0.getStepExecutionId(),
                java.util.Arrays.toString(stepExecution0.getMetrics()));

        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.ROLLBACK_COUNT));
        assertEquals(12, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
        assertEquals(30, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        assertEquals(30, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.PROCESS_SKIP_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_SKIP_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_SKIP_COUNT));
        assertEquals(0, MetricImpl.getMetric(stepExecution0, Metric.MetricType.FILTER_COUNT));
    }
}
