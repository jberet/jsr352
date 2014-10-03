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

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * The base class for {@link JmsItemReader} and {@link JmsItemWriter}.
 */
public abstract class JmsItemReaderWriterBase extends ItemReaderWriterBase {
    /**
     * This field holds an optional injection of {@code javax.jms.Destination}. When {@link #destinationLookupName}
     * property is specified in job xml, this field is ignored and {@link #destinationLookupName} is used to look up
     * JMS destination. The application may implement a {@code javax.enterprise.inject.Produces} method to satisfy
     * this dependency injection.
     */
    @Inject
    protected Instance<Destination> destinationInstance;

    /**
     * This field holds an optional injection of {@code javax.jms.ConnectionFactory}. When {@link #connectionFactoryLookupName}
     * property is specified in job xml, this field is ignored and {@link #connectionFactoryLookupName} is used to
     * look up JMS {@code ConnectionFactory}. The application may implement a {@code javax.enterprise.inject.Produces}
     * method to satisfy this dependency injection.
     */
    @Inject
    protected Instance<ConnectionFactory> connectionFactoryInstance;

    /**
     * JNDI lookup name for the JMS {@code Destination}. Optional property and defaults to null. When specified in
     * job xml, it has higher precedence over {@link #destinationInstance} injection
     */
    @Inject
    @BatchProperty
    protected String destinationLookupName;

    /**
     * JNDI lookup name for the JMS {@code ConnectionFactory}. Optional property and defaults to null. When specified in
     * job xml, it has higher precedence over {@link #connectionFactoryInstance} injection.
     */
    @Inject
    @BatchProperty
    protected String connectionFactoryLookupName;

    /**
     * The string name of the sessionMode used to create JMS session from a JMS connection. Optional property, and
     * defaults to null. When not specified, JMS API {@link javax.jms.Connection#createSession()} is invoked to create
     * the JMS session. When this property is specified, its value must be either {@code AUTO_ACKNOWLEDGE} or
     * {@code DUPS_OK_ACKNOWLEDGE}.
     * <p/>
     * An example property in job xml:
     * <p/>
     * &lt;property name="sessionMode" value="DUPS_OK_ACKNOWLEDGE"/&gt;
     * <p/>
     * See JMS API {@link javax.jms.Connection#createSession(int)} for more details.
     */
    @Inject
    @BatchProperty
    protected String sessionMode;

    protected Destination destination;
    protected ConnectionFactory connectionFactory;
    protected Connection connection;
    protected Session session;

    public void open(final Serializable checkpoint) throws Exception {
        InitialContext ic = null;
        try {
            if (destinationLookupName != null) {
                ic = new InitialContext();
                destination = (Destination) ic.lookup(destinationLookupName);
            } else {
                destination = destinationInstance.get();
            }
            if (connectionFactoryLookupName != null) {
                if (ic == null) {
                    ic = new InitialContext();
                }
                connectionFactory = (ConnectionFactory) ic.lookup(connectionFactoryLookupName);
            } else {
                connectionFactory = connectionFactoryInstance.get();
            }
            connection = connectionFactory.createConnection();

            if (sessionMode != null) {
                final int sessionModeInt;
                if (sessionMode.equals("AUTO_ACKNOWLEDGE")) {
                    sessionModeInt = Session.AUTO_ACKNOWLEDGE;
                } else if (sessionMode.equals("DUPS_OK_ACKNOWLEDGE")) {
                    sessionModeInt = Session.DUPS_OK_ACKNOWLEDGE;
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, sessionMode, "sessionMode");
                }
                session = connection.createSession(sessionModeInt);
            } else {
                session = connection.createSession();
            }
        } finally {
            if (ic != null) {
                ic.close();
            }
        }
    }

    protected void close() {
        if (session != null) {
            try {
                session.close();
            } catch (final JMSException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close JMS session %s%n", session);
            }
            session = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (final JMSException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close JMS connection %s%n", connection);
            }
            connection = null;
        }
    }
}
