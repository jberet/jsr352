/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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
