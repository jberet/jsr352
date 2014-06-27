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
import java.util.List;
import java.util.Map;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that sends data items to a JMS destination. It can
 * sends the following JMS message types:
 * <p/>
 * <ul>
 * <li>if the data item is of type {@code java.util.Map}, a {@code MapMessage} is created, populated with the data
 * contained in the data item, and sent;</li>
 * <li>else if the data item is of type {@code java.lang.String}, a {@code TextMessage} is created with the text content
 * in the data item, and sent;</li>
 * <li>else if the data is of type {@code javax.jms.Message}, it is sent as is;</li>
 * <li>else an {@code ObjectMessage} is created with the data item object, and sent.</li>
 * </ul>
 * <p/>
 *
 * @see JmsItemReader
 * @since 1.1.0
 */
@Named
@Dependent
public class JmsItemWriter extends JmsItemReaderWriterBase implements ItemWriter {
    protected MessageProducer producer;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        producer = session.createProducer(destination);
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        for (final Object item : items) {
            final Message msg;
            if (item instanceof Map) {
                final Map<?, ?> itemAsMap = (Map) item;
                final MapMessage mapMessage = session.createMapMessage();
                for (final Map.Entry e : itemAsMap.entrySet()) {
                    mapMessage.setObject(e.getKey().toString(), e.getValue());
                }
                msg = mapMessage;
            } else if (item instanceof String) {
                msg = session.createTextMessage((String) item);
            } else if (item instanceof Message) {
                msg = (Message) item;
            } else {
                msg = session.createObjectMessage((Serializable) item);
            }
            producer.send(msg);
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (producer != null) {
            try {
                producer.close();
            } catch (final JMSException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close JMS consumer %s%n", producer);
            }
            producer = null;
        }
    }
}
