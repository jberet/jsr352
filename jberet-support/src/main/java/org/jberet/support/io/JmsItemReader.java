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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

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

    protected MessageConsumer consumer;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        consumer = session.createConsumer(destination);
    }

    @Override
    public Object readItem() throws Exception {
        final Object result;
        final Message message = consumer.receive(receiveTimeout);
        if (beanType == Map.class) {
            if (message instanceof MapMessage) {
                final Map<String, Object> mapResult = new HashMap<String, Object>();
                final MapMessage mapMessage = (MapMessage) message;
                final Enumeration mapNames = mapMessage.getMapNames();
                while (mapNames.hasMoreElements()) {
                    final String k = (String) mapNames.nextElement();
                    mapResult.put(k, mapMessage.getObject(k));
                }
                result = mapResult;
            } else {
                throw SupportMessages.MESSAGES.unexpectedJmsMessageType("MapMessage", message.getJMSType(), message.toString());
            }
        } else {
            if (message instanceof ObjectMessage) {
                final ObjectMessage objectMessage = (ObjectMessage) message;
                result = objectMessage.getObject();
            } else {
                throw SupportMessages.MESSAGES.unexpectedJmsMessageType("ObjectMessage", message.getJMSType(), message.toString());
            }
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
