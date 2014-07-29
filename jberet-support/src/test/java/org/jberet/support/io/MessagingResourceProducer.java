/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;

@Named
public class MessagingResourceProducer {
    // JMS resources
    static ConnectionFactory connectionFactory;
    static Queue queue;

    // HornetQ resources
    static ServerLocator serverLocator;
    static ClientSessionFactory sessionFactory;

    @Produces
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Produces
    public Destination getDestination() {
        return queue;
    }

    @Produces
    public ServerLocator getServerLocator() {
        return serverLocator;
    }

    @Produces
    public ClientSessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
