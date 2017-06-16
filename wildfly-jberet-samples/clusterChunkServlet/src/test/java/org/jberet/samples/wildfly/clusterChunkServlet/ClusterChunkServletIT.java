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

package org.jberet.samples.wildfly.clusterChunkServlet;

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
public final class ClusterChunkServletIT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/timer-scheduler-job1.xml}
     */
    private static final String clusterChunkServlet = "clusterChunkServlet";
    private static final String clusterChunkServletStop = "clusterChunkServletStop";
    private static final long waitForCompletionMillis = 60000;

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "clusterChunkServlet/api";

    private BatchClient batchClient = new BatchClient(restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void clusterChunkServlet() throws Exception {
        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(clusterChunkServlet, null);
        Thread.sleep(waitForCompletionMillis);
        final JobExecutionEntity jobExecution1 = batchClient.getJobExecution(jobExecutionEntity.getExecutionId());
        assertEquals(BatchStatus.COMPLETED, jobExecution1.getBatchStatus());

        final StepExecutionEntity[] stepExecutions = batchClient.getStepExecutions(jobExecution1.getExecutionId());
        assertEquals(1, stepExecutions.length);
        final StepExecutionEntity stepExecutionEntity = stepExecutions[0];
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecutionEntity.getBatchStatus());

    }
}
