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

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.SendAcknowledgementHandler;
import org.hornetq.api.core.client.ServerLocator;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * The base class for {@link org.jberet.support.io.HornetQItemReader} and {@link org.jberet.support.io.HornetQItemWriter}.
 */
public abstract class HornetQItemReaderWriterBase {
    protected static final String QUEUE_NAME_KEY = "name";
    protected static final String QUEUE_ADDRESS_KEY = "address";
    protected static final String QUEUE_FILTER_KEY = "filter";
    protected static final String QUEUE_DURABLE_KEY = "durable";
    protected static final String QUEUE_SHARED_KEY = "shared";
    protected static final String QUEUE_TEMPORARY_KEY = "temporary";

    @Inject
    protected Instance<ServerLocator> serverLocatorInstance;

    @Inject
    protected Instance<ClientSessionFactory> sessionFactoryInstance;

    @Inject
    @BatchProperty
    protected boolean withHA;

    @Inject
    @BatchProperty
    protected String connectorFactoryName;

    /**
     * Key-value pairs to configure HornetQ {@code ServerLocator}. Optional property and defaults to null. See the
     * current version of HornetQ {@code ServerLocator} javadoc for supported keys and values, e.g.,
     * <a href="http://docs.jboss.org/hornetq/2.4.0.Final/docs/api/hornetq-client/org/hornetq/api/core/client/ServerLocator.html">ServerLocator</a>
     * <p/>
     * For example,
     * <p/>
     * &lt;property name="serverLocatorParams" value="AckBatchSize=5, ProducerMaxRate=10"/&gt;
     */
    @Inject
    @BatchProperty
    protected Map serverLocatorParams;

    @Inject
    @BatchProperty
    protected Map connectorFactoryParams;

    @Inject
    @BatchProperty
    protected Map queueParams;

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

        if (connectorFactoryName != null) {
            if (withHA) {
                serverLocator = connectorFactoryParams == null ?
                        HornetQClient.createServerLocatorWithHA(new TransportConfiguration(connectorFactoryName)) :
                        HornetQClient.createServerLocatorWithHA(new TransportConfiguration(connectorFactoryName, connectorFactoryParams));
            } else {
                serverLocator = connectorFactoryParams == null ?
                        HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(connectorFactoryName)) :
                        HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(connectorFactoryName, connectorFactoryParams));
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
        //createQueue();
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
            } catch (final HornetQException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close HornetQ client core session %s%n", session);
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
