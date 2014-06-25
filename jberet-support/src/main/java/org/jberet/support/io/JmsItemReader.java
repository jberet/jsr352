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
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import org.jberet.support._private.SupportLogger;

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
        return consumer.receive(receiveTimeout);
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
