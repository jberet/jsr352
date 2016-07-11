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

package org.jberet.samples.wildfly.camelReaderWriter;

import java.net.URI;
import javax.batch.runtime.BatchStatus;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Assert;
import org.junit.Test;

public final class CamelReaderWriterIT extends BatchTestBase {

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "camelReaderWriter/api";

    private Client client = ClientBuilder.newClient();

    private BatchClient batchClient = new BatchClient(client, restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void testCamelWriter() throws Exception {
        runTest("/camel/writer", 3000);
    }

    @Test
    public void testCamelReader() throws Exception {
        runTest("/camel/reader", 40000);
    }

    @Test
    public void testCamelProcessor() throws Exception {
        runTest("/camel/processor", 3000);
    }

    @Test
    public void testCamelComponent() throws Exception {
        runTest("/camel/component", 3000);
    }

    private void runTest(final String resourceUrl, final long waitForJobCompletionMillis) throws Exception {
        final WebTarget target = client.target(new URI(restUrl + resourceUrl));
        final long jobExecutionId = target.request().get(long.class);
        System.out.printf("Job execution id in response: %s%n", jobExecutionId);

        Thread.sleep(waitForJobCompletionMillis);
        final JobExecutionEntity jobExecutionEntity = batchClient.getJobExecution(jobExecutionId);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecutionEntity.getBatchStatus());
    }

}
