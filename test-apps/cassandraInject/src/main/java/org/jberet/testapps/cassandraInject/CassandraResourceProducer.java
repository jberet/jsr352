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

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import com.datastax.driver.core.Session;

/**
 * A CDI producer class for Cassandra cluster session resource.
 *
 * @since 1.3.0.Final
 */
@Named
public class CassandraResourceProducer {
    static Session session;

    @Produces
    public Session getSession() {
        return session;
    }
}
