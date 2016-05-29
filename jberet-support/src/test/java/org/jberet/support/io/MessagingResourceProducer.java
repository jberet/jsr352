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

package org.jberet.support.io;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;

import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.ServerLocator;

@Named
public class MessagingResourceProducer {
    // JMS resources
    static ConnectionFactory connectionFactory;
    static Queue queue;

    // HornetQ resources
    static ServerLocator hornetQServerLocator;
    static ClientSessionFactory hornetQSessionFactory;

    // Artemis resources
    static org.apache.activemq.artemis.api.core.client.ServerLocator artemisServerLocator;
    static org.apache.activemq.artemis.api.core.client.ClientSessionFactory artemisSessionFactory;


    @Produces
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Produces
    public Destination getDestination() {
        return queue;
    }

    @Produces
    public ServerLocator getHornetQServerLocator() {
        return hornetQServerLocator;
    }

    @Produces
    public ClientSessionFactory getHornetQSessionFactory() {
        return hornetQSessionFactory;
    }

    @Produces
    public org.apache.activemq.artemis.api.core.client.ServerLocator getArtemisServerLocator() {
        return artemisServerLocator;
    }

    @Produces
    public org.apache.activemq.artemis.api.core.client.ClientSessionFactory getArtemisSessionFactory() {
        return artemisSessionFactory;
    }
}
