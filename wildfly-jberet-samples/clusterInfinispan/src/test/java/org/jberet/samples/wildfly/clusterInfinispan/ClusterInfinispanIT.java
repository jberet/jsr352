/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.clusterInfinispan;

import javax.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.StepExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Ignore("need to configure and start WildFly cluster first")
public final class ClusterInfinispanIT extends BatchTestBase {
    private static final String clusterChunkInfinispan = "clusterChunkInfinispan";
    private static final String clusterBatchletInfinispan = "clusterBatchletInfinispan";
    private static final String clusterChunkInfinispanStop = "clusterChunkInfinispanStop";
    private static final String clusterBatchletInfinispanStop = "clusterBatchletInfinispanStop";
    private static final long waitForCompletionMillis = 60000;

    protected static final String BASE_URL_1 = "http://localhost:8230/";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL_1 + "clusterInfinispan/api";

    private BatchClient batchClient = new BatchClient(restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void clusterChunkInfinispan() throws Exception {
        runChunkOrBatchletJob(clusterChunkInfinispan);
    }

    @Test
    public void clusterBatchletInfinispan() throws Exception {
        runChunkOrBatchletJob(clusterBatchletInfinispan);
    }

    @Test
    public void stopRemoteChunkPartitions() throws Exception {
        stopRemotePartitions(clusterChunkInfinispanStop);
    }

    @Test
    public void stopRemoteBatchletPartitions() throws Exception {
        stopRemotePartitions(clusterBatchletInfinispanStop);
    }

    private void runChunkOrBatchletJob(final String jobName) throws Exception {
        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(jobName, null);
        Thread.sleep(waitForCompletionMillis);
        final JobExecutionEntity jobExecution1 = batchClient.getJobExecution(jobExecutionEntity.getExecutionId());
        assertEquals(BatchStatus.COMPLETED, jobExecution1.getBatchStatus());

        final StepExecutionEntity[] stepExecutions = batchClient.getStepExecutions(jobExecution1.getExecutionId());
        assertEquals(1, stepExecutions.length);
        final StepExecutionEntity stepExecutionEntity = stepExecutions[0];
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecutionEntity.getBatchStatus());
    }

    private void stopRemotePartitions(final String jobName) throws Exception {
        JobExecutionEntity jobExecutionEntity = batchClient.startJob(jobName, null);
        Thread.sleep(20000);
        batchClient.stopJobExecution(jobExecutionEntity.getExecutionId());

        Thread.sleep(120000);

        jobExecutionEntity = batchClient.getJobExecution(jobExecutionEntity.getExecutionId());
        final StepExecutionEntity[] stepExecutions = batchClient.getStepExecutions(jobExecutionEntity.getExecutionId());
        final StepExecutionEntity stepExecution = stepExecutions[0];

        System.out.printf("job batch status: %s, step batch status: %s%n",
                jobExecutionEntity.getBatchStatus(), stepExecution.getBatchStatus());
        assertEquals(BatchStatus.STOPPED, jobExecutionEntity.getBatchStatus());
        assertEquals(BatchStatus.STOPPED, stepExecution.getBatchStatus());
    }
}
