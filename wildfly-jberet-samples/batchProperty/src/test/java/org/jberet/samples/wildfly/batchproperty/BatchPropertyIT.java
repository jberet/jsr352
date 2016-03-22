/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.batchproperty;

import javax.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Test;

public class BatchPropertyIT extends BatchTestBase {
    private static final String jobName = "batchproperty";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "batchproperty/api";

    private BatchClient batchClient = new BatchClient(client, restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void testBatchProperty() throws Exception {
        startJobCheckStatus(jobName, null, 500, BatchStatus.COMPLETED);
    }
}
