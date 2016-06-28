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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jberet.rest.client.BatchClient;
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
        final WebTarget target = client.target(new URI(restUrl + "/camel/writer"));
        final Response response = target.request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Object responseEntity = response.getEntity();
        System.out.printf("Job execution id in response: %s%n", responseEntity);
    }

    @Test
    public void testCamelReader() throws Exception {
        final WebTarget target = client.target(new URI(restUrl + "/camel/reader"));
        final Response response = target.request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Object responseEntity = response.getEntity();
        System.out.printf("Job execution id in response: %s%n", responseEntity);
    }
}
