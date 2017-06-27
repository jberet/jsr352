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

package org.jberet.samples.wildfly.clusterChunkEJB;

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
public final class ClusterChunkEJBIT extends BatchTestBase {
    private static final String clusterChunkEJB = "clusterChunkEJB";
    private static final String clusterChunkEJBStop = "clusterChunkEJBStop";
    private static final long waitForCompletionMillis = 60000;

    protected static final String BASE_URL_1 = "http://localhost:8230/";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL_1 + "clusterChunkEJB/api";

    private BatchClient batchClient = new BatchClient(restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void clusterChunkEJB() throws Exception {
        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(clusterChunkEJB, null);
        Thread.sleep(waitForCompletionMillis);
        final JobExecutionEntity jobExecution1 = batchClient.getJobExecution(jobExecutionEntity.getExecutionId());
        assertEquals(BatchStatus.COMPLETED, jobExecution1.getBatchStatus());

        final StepExecutionEntity[] stepExecutions = batchClient.getStepExecutions(jobExecution1.getExecutionId());
        assertEquals(1, stepExecutions.length);
        final StepExecutionEntity stepExecutionEntity = stepExecutions[0];
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecutionEntity.getBatchStatus());

    }

    @Test
    public void stopRemotePartitions() throws Exception {
        JobExecutionEntity jobExecutionEntity = batchClient.startJob(clusterChunkEJBStop, null);
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
