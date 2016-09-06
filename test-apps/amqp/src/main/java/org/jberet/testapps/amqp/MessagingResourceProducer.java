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

package org.jberet.testapps.amqp;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;

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