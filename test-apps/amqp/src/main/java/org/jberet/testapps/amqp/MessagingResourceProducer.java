/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.amqp;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@Named
public class MessagingResourceProducer {
    // JMS resources
    static ConnectionFactory connectionFactory;
    static Queue queue;

    @Produces
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Produces
    public Destination getDestination() {
        return queue;
    }

}