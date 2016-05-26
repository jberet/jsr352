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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.SendAcknowledgementHandler;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * The base class for {@link ArtemisItemReader} and {@link ArtemisItemWriter}.
 *
 * @see     ArtemisItemReader
 * @see     ArtemisItemWriter
 * @since   1.1.0
 */
public abstract class ArtemisItemReaderWriterBase extends ItemReaderWriterBase {
    protected static final String QUEUE_NAME_KEY = "name";
    protected static final String QUEUE_ADDRESS_KEY = "address";
    protected static final String QUEUE_FILTER_KEY = "filter";
    protected static final String QUEUE_DURABLE_KEY = "durable";
    protected static final String QUEUE_SHARED_KEY = "shared";
    protected static final String QUEUE_TEMPORARY_KEY = "temporary";

    protected static final String SERVER_LOCATOR_HA_KEY = "HA";
    protected static final String NAME_KEY = "name";
    protected static final String FACTORY_CLASS_KEY = "factory-class";

    /**
     * This field holds an optional injection of Artemis {@code ServerLocator}. When {@link #connectorFactoryParams} is
     * not specified, and {@link #sessionFactoryInstance} is not satisfied, this field will be queried to obtain an
     * instance of Artemis {@code ServerLocator}. The application may implement a
     * {@code javax.enterprise.inject.Produces} method to satisfy this dependency injection.
     */
    @Inject
    protected Instance<ServerLocator> serverLocatorInstance;

    /**
     * This field holds an optional injection of Artemis {@code ClientSessionFactory}. If this injection is satisfied,
     * {@link #serverLocatorInstance} will be ignored. The application may implement a
     * {@code javax.enterprise.inject.Produces} method to satisfy this dependency injection.
     */
    @Inject
    protected Instance<ClientSessionFactory> sessionFactoryInstance;

    /**
     * Key-value pairs to identify and configure Artemis {@code org.apache.activemq.artemis.api.core.TransportConfiguration}, which is
     * used to create Artemis {@code ServerLocator}. Optional property and defaults to null. When this property is
     * present, it will be used to create Artemis {@code ServerLocator}, and the injection fields
     * {@link #serverLocatorInstance} and {@link #sessionFactoryInstance} will be ignored. Valid keys and values are:
     * <p>
     * <ul>
     *     <li>{@value #FACTORY_CLASS_KEY}, the fully-qualified class name of a Artemis connector factory, required if this property is present
     *     <li>any param keys and values appropriate for the above-named Artemis connector factory class
     * </ul>
     * <p>
     * An example of this property in job xml:
     * <p>
     *  &lt;property name="connectorFactoryParams"
     *  value="factory-class=org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory, host=localhost, port=5445"/&gt;
     */
    @Inject
    @BatchProperty
    protected Map connectorFactoryParams;

    /**
     * Key-value pairs to configure Artemis {@code ServerLocator}. Optional property and defaults to null.
     * Valid keys are:
     * <p>
     * <ul>
     * <li>HA: true or false (default), true if the {@code ServerLocator} receives topology updates from the cluster
     * <li>Properties in {@code ServerLocator} class that have corresponding setter method, starting with either
     * upper or lower case character
     * </ul>
     * <p>
     * See the current version of Artemis {@code ServerLocator} javadoc for supported keys and values, e.g.,
     * <a href="http://activemq.apache.org/artemis/docs/javadocs/javadoc-1.2.0/org/apache/activemq/artemis/api/core/client/ServerLocator.html">ServerLocator</a>
     * <p>
     * An example of this property in job xml:
     * <p>
     * &lt;property name="serverLocatorParams" value="HA=false, AckBatchSize=5, ProducerMaxRate=10, BlockOnAcknowledge=false, ConfirmationWindowSize=5"/&gt;
     */
    @Inject
    @BatchProperty
    protected Map serverLocatorParams;

    /**
     * Key-value pairs to identify and configure the target Artemis queue. Required property.
     * <p>
     * The following keys are supported:
     * <p>
     * <ul>
     * <li>{@value #QUEUE_ADDRESS_KEY}, required
     * <li>{@value #QUEUE_DURABLE_KEY}, optional
     * <li>{@value #QUEUE_FILTER_KEY}, optional
     * <li>{@value #QUEUE_NAME_KEY}, optional
     * <li>{@value #QUEUE_SHARED_KEY}, optional
     * <li>{@value #QUEUE_TEMPORARY_KEY}, optional
     * </ul>
     * <p>
     * An example of {@code queueParams} property in job xml:
     * <p>
     * &lt;property name="queueParams" value="address=example, durable=false"/&gt;
     */
    @Inject
    @BatchProperty
    protected Map queueParams;

    /**
     * The fully-qualified name of a class that implements {@code org.apache.activemq.artemis.api.core.client.SendAcknowledgementHandler}.
     * A SendAcknowledgementHandler notifies a client when an message sent asynchronously has been received by the server.
     * See current version of Artemis documentation for details, e.g.,
     * <a href="http://activemq.apache.org/artemis/docs/javadocs/javadoc-1.2.0/org/apache/activemq/artemis/api/core/client/SendAcknowledgementHandler.html">SendAcknowledgementHandler</a>
     * <p>
     * An example {@code sendAcknowledgementHandler} property in job xml:
     * <p>
     * &lt;property name="sendAcknowledgementHandler" value="org.jberet.support.io.ArtemisReaderWriterTest$ArtemisSendAcknowledgementHandler"/&gt;
     */
    @Inject
    @BatchProperty
    protected Class sendAcknowledgementHandler;

    protected SimpleString queueAddress;
    protected SimpleString queueName;
    protected ServerLocator serverLocator;
    protected ClientSessionFactory sessionFactory;
    protected ClientSession session;

    private boolean toCloseServerLocator;
    private boolean toCloseSessionFactory;

    public void open(final Serializable checkpoint) throws Exception {
        if (queueParams == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "queueParams");
        }
        queueAddress = SimpleString.toSimpleString((String) queueParams.get(QUEUE_ADDRESS_KEY));
        queueName = SimpleString.toSimpleString((String) queueParams.get(QUEUE_NAME_KEY));
        if (queueName == null) {
            queueName = queueAddress;
        }

        if (connectorFactoryParams != null) {
            final String connectorFactoryName = (String) connectorFactoryParams.get(NAME_KEY);
            if (connectorFactoryName == null) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, connectorFactoryParams.toString(), "connectorFactoryParams");
            }
            connectorFactoryParams.remove(NAME_KEY);

            boolean withHA = false;
            if (serverLocatorParams != null) {
                if (serverLocatorParams.containsKey(SERVER_LOCATOR_HA_KEY)) {
                    withHA = Boolean.parseBoolean((String) serverLocatorParams.get(SERVER_LOCATOR_HA_KEY));
                    if (serverLocatorParams.size() == 1) {
                        serverLocatorParams = null;
                    } else {
                        serverLocatorParams.remove(SERVER_LOCATOR_HA_KEY);
                    }
                }
            }
            if (withHA) {
                serverLocator = connectorFactoryParams == null ?
                        ActiveMQClient.createServerLocatorWithHA(new TransportConfiguration(connectorFactoryName)) :
                        ActiveMQClient.createServerLocatorWithHA(new TransportConfiguration(connectorFactoryName, connectorFactoryParams));
            } else {
                serverLocator = connectorFactoryParams == null ?
                        ActiveMQClient.createServerLocatorWithoutHA(new TransportConfiguration(connectorFactoryName)) :
                        ActiveMQClient.createServerLocatorWithoutHA(new TransportConfiguration(connectorFactoryName, connectorFactoryParams));
            }
            toCloseServerLocator = true;
        } else {
            if (sessionFactoryInstance.isUnsatisfied()) {
                serverLocator = serverLocatorInstance.get();
            } else {
                sessionFactory = sessionFactoryInstance.get();
            }
        }

        if (serverLocatorParams != null && serverLocator != null) {
            configureServerLocator();
            sessionFactory = serverLocator.createSessionFactory();
            toCloseSessionFactory = true;
        }

        session = sessionFactory.createSession();
        if (sendAcknowledgementHandler != null) {
            session.setSendAcknowledgementHandler((SendAcknowledgementHandler) sendAcknowledgementHandler.newInstance());
        }
    }

    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    protected void configureServerLocator() throws Exception {
        for (final Object o : serverLocatorParams.keySet()) {
            final String key = (String) o;
            final String val = (String) serverLocatorParams.get(key);
            final String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);

            final Method method = ServerLocator.class.getMethod(setterName);
            final Class<?> param1 = method.getParameterTypes()[0];
            final Object[] args = new Object[1];
            if (param1 == int.class) {
                args[0] = Integer.valueOf(val);
            } else if (param1 == String.class) {
                args[0] = val;
            } else if (param1 == boolean.class) {
                args[0] = Boolean.valueOf(val);
            } else if (param1 == long.class) {
                args[0] = Long.valueOf(val);
            } else if (param1 == double.class) {
                args[0] = Double.valueOf(val);
            }
            method.invoke(serverLocator, args);
        }
    }

    protected void close() {
        if (session != null) {
            try {
                session.close();
            } catch (final ActiveMQException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close Artemis client core session %s%n", session);
            }
            session = null;
        }
        if (sessionFactory != null && toCloseSessionFactory) {
            sessionFactory.close();
            sessionFactory = null;
        }
        if (serverLocator != null && toCloseServerLocator) {
            serverLocator.close();
            serverLocator = null;
        }
    }

    protected static byte[] objectToBytes(final Object obj) throws IOException {
        if (obj == null) {
            return null;
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            return bos.toByteArray();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                bos.close();
            } catch (IOException e2) {
                //ignore
            }
        }
    }

    protected static Serializable bytesToSerializableObject(final byte[] bytes) throws IOException, ClassNotFoundException {
        if (bytes == null) {
            return null;
        }
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(bis);
            return (Serializable) in.readObject();
        } finally {
            try {
                bis.close();
                if (in != null) {
                    in.close();
                }
            } catch (IOException e2) {
                //ignore
            }
        }
    }
}
