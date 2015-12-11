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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads data items from a JMS destination. It can
 * reads the following JMS message types:
 * <p>
 * <ul>
 * <li>{@code ObjectMessage}: the object contained in the message is retrieved and returned;
 * <li>{@code MapMessage}: a new {@code java.util.Map} is created, populated with the data contained in the
 * incoming {@code MapMessage}, and returned;
 * <li>{@code TextMessage}: the text contained in the message is retrieved and returned;
 * <li>{@code Message}: but not one of its subtype, null is returned.
 * </ul>
 * <p>
 * If {@link #beanType} is set to {@code javax.jms.Message}, {@link #readItem()} returns the incoming JMS message as is.
 * Otherwise, {@link #readItem()} method determines the actual data type based on the message type.
 * <p>
 * This reader ends when any of the following occurs:
 * <ul>
 * <li>{@link #receiveTimeout} (in milliseconds) has elapsed when trying to receive a message from the destination;
 * <li>any {@code null} body is retrieved from a message;
 * <li>any message is of type {@code Message}, but not one of its subtype.
 * </ul>
 *
 * @see     JmsItemWriter
 * @see     JmsItemReaderWriterBase
 * @since   1.1.0
 */
@Named
@Dependent
public class JmsItemReader extends JmsItemReaderWriterBase implements ItemReader {
    /**
     * The number of milliseconds a JMS {@code MessageConsumer} blocks until a message arrives. Optional property, and
     * defaults to 0, which means it blocks indefinitely.
     */
    @Inject
    @BatchProperty
    protected long receiveTimeout;

    /**
     * Only messages with properties matching the message selector expression are delivered. A value of null or an
     * empty string indicates that there is no message selector for the message consumer.
     * See JMS API {@link javax.jms.Session#createConsumer(javax.jms.Destination, java.lang.String)}
     */
    @Inject
    @BatchProperty
    protected String messageSelector;

    /**
     * The fully-qualified class name of the data item to be returned from {@link #readItem()} method. Optional
     * property and defaults to null. If it is specified, its valid value is:
     * <p>
     * <ul>
     * <li>{@code javax.jms.Message}: an incoming JMS message is returned as is.
     * </ul>
     * <p>
     * When this property is not specified, {@link #readItem()} method returns an object whose actual type is
     * determined by the incoming JMS message type.
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    protected MessageConsumer consumer;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        consumer = session.createConsumer(destination, messageSelector);
        connection.start();
    }

    @Override
    public Object readItem() throws Exception {
        final Object result;
        final Message message = consumer.receive(receiveTimeout);
        if (message == null) {  //no more messages after receiveTimeout
            return null;
        }

        if (message.getBody(Object.class) == null) {
            return null;
        }

        if (beanType == Message.class) {
            return message;
        }

        if (message instanceof ObjectMessage) {
            final ObjectMessage objectMessage = (ObjectMessage) message;
            result = objectMessage.getObject();
            if (!skipBeanValidation) {
                ItemReaderWriterBase.validate(result);
            }
        } else if (message instanceof MapMessage) {
            final Map<String, Object> mapResult = new HashMap<String, Object>();
            final MapMessage mapMessage = (MapMessage) message;
            final Enumeration mapNames = mapMessage.getMapNames();
            while (mapNames.hasMoreElements()) {
                final String k = (String) mapNames.nextElement();
                mapResult.put(k, mapMessage.getObject(k));
            }
            result = mapResult;
        } else if (message instanceof TextMessage) {
            final TextMessage textMessage = (TextMessage) message;
            result = textMessage.getText();
        } else {
            throw SupportMessages.MESSAGES.unexpectedJmsMessageType("ObjectMessage | MapMessage | TextMessage", message.getJMSType(), message.toString());
        }

        return result;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (consumer != null) {
            try {
                consumer.close();
            } catch (final JMSException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close JMS consumer %s%n", consumer);
            }
            consumer = null;
        }
    }
}
