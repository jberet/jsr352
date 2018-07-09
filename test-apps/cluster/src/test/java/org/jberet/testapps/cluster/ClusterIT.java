/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cluster;

import java.util.List;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Ignore("Need to manually run other Vert.x nodes first")
public class ClusterIT extends AbstractIT {
    private static final String clusterJob = "clusterJob.xml";
    private static final String clusterJobStop = "clusterJobStop.xml";

    @Test
    public void readArrayWriteToConsole() throws Exception {
        startJobAndWait(clusterJob);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());

        final List<PartitionExecutionImpl> partitionExecutions = stepExecution0.getPartitionExecutions();
        for (PartitionExecutionImpl p : partitionExecutions) {
            System.out.printf("partition %s batch status: %s%n", p.getPartitionId(), p.getBatchStatus());
            assertEquals(BatchStatus.COMPLETED, p.getBatchStatus());
        }
    }

    @Test
    public void stopRemotePartitions() throws Exception {
        startJob(clusterJobStop);
        Thread.sleep(20000);
        jobOperator.stop(jobExecutionId);

        Thread.sleep(120000);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        final StepExecutionImpl stepExecution = (StepExecutionImpl) jobExecution.getStepExecutions().get(0);

        System.out.printf("job batch status: %s, step batch status: %s%n",
                jobExecution.getBatchStatus(), stepExecution.getBatchStatus());
        assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.STOPPED, stepExecution.getBatchStatus());

        final List<PartitionExecutionImpl> partitionExecutions = stepExecution.getPartitionExecutions();
        for (PartitionExecutionImpl p : partitionExecutions) {
            System.out.printf("partition %s batch status: %s%n", p.getPartitionId(), p.getBatchStatus());
            assertEquals(BatchStatus.STOPPED, p.getBatchStatus());
        }
    }
}
