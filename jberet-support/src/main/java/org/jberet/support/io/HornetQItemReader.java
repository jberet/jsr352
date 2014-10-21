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
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads data items from a HornetQ address.
 * It handles the following types of HornetQ messages:
 * <p/>
 * <ul>
 * <li>if {@link #beanType} is set to {@code org.hornetq.api.core.client.ClientMessage}, the incoming message
 * is immediately returned as is from {@link #readItem()}</li>
 * <li>if the message type is {@code org.hornetq.api.core.client.ClientMessage#TEXT_TYPE}, the string text is
 * returned from {@link #readItem()};</li>
 * <li>otherwise, a byte array is retrieved from the message body buffer, deserialize to an object, and returned
 * from {@link #readItem()}</li>
 * </ul>
 * <p/>
 * This reader ends when either of the following occurs:
 * <p/>
 * <ul>
 * <li>{@link #receiveTimeout} (in milliseconds) has elapsed when trying to receive a message from the destination;</li>
 * <li>the size of the incoming message body is 0;</li>
 * </ul>
 *
 * @see     HornetQItemWriter
 * @see     HornetQItemReaderWriterBase
 * @see     JmsItemReader
 * @since   1.1.0
 */
@Named
@Dependent
public class HornetQItemReader extends HornetQItemReaderWriterBase implements ItemReader {
    /**
     * The number of milliseconds a HornetQ {@code ClientConsumer} blocks until a message arrives. Optional property, and
     * defaults to 0, which means it blocks indefinitely.
     */
    @Inject
    @BatchProperty
    protected long receiveTimeout;

    /**
     * The fully-qualified class name of the data item to be returned from {@link #readItem()} method. Optional
     * property and defaults to null. If it is specified, its valid value is:
     * <p/>
     * <ul>
     * <li>{@code org.hornetq.api.core.client.ClientMessage}: an incoming HornetQ message is returned as is.</li>
     * </ul>
     * <p/>
     * When this property is not specified, {@link #readItem()} method returns an object whose actual type is
     * determined by the incoming HornetQ message type.
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    protected ClientConsumer consumer;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        consumer = session.createConsumer(queueName);
        session.start();
    }

    @Override
    public Object readItem() throws Exception {
        final Object result;
        final ClientMessage message = consumer.receive(receiveTimeout);
        if (message == null) {  //no more messages after receiveTimeout
            return null;
        }

        final int bodySize = message.getBodySize();
        if (bodySize == 0) {
            return null;
        }

        if (beanType == ClientMessage.class) {
            return message;
        }

        final byte messageType = message.getType();
        final byte[] bytes = new byte[bodySize];
        message.getBodyBuffer().readBytes(bytes);
        if (messageType == ClientMessage.TEXT_TYPE) {
            result = new String(bytes);
        } else {
            result = bytesToSerializableObject(bytes);
            if (!skipBeanValidation) {
                ItemReaderWriterBase.validate(result);
            }
        }

        return result;
    }

    @Override
    public void close() {
        super.close();
        if (consumer != null) {
            try {
                consumer.close();
            } catch (final HornetQException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close HornetQ consumer %s%n", consumer);
            }
            consumer = null;
        }
    }
}
