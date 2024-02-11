/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cassandraInject;

import jakarta.batch.runtime.BatchStatus;

import com.datastax.driver.core.Cluster;
import org.jberet.testapps.common.AbstractIT;
import org.junit.AfterClass;
import org.junit.jupiter.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

/**
 * Tests using {@code cassandraItemReader} and {@code mockItemWriter} in jberet-support.
 * In these tests, Cassandra session is injected into {@code cassandraItemReader}.
 *
 * @since 1.3.0.Final
 */
@Ignore("Need to run Cassandra cluster in a separate process")
public class CassandraInjectIT extends AbstractIT {
    static final String contactPoints = "localhost";

    private static final String simpleJob = "cassandraInject.xml";

    private static final String cql = "select key, cluster_name, listen_address, cql_version from system.local";

    @BeforeClass
    public static void beforeClass() {
        if (CassandraResourceProducer.session == null) {
            Cluster cluster = new Cluster.Builder().addContactPoint(contactPoints).build();
            CassandraResourceProducer.session = cluster.newSession();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (CassandraResourceProducer.session != null) {
            CassandraResourceProducer.session.close();
        }
    }

    /**
     * This test reads from Cassandra system local table with {@code cassandraItemReader},
     * and displays the data to console with {@code mockItemWriter}. A sample output:
     * <pre>
     *     [local, Test Cluster, /127.0.0.1, 3.4.4]
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void readCassandraWriteToConsole() throws Exception {
        params.setProperty("beanType", java.util.List.class.getName());
        params.setProperty("cql", cql);
        startJobAndWait(simpleJob);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
