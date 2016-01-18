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

package org.jberet.support.io;

import java.util.Map;
import javax.batch.operations.BatchRuntimeException;

import org.apache.kafka.common.serialization.Deserializer;
import org.jberet.util.BatchUtil;

public class StockTradeDeserializer implements Deserializer<StockTrade> {
    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {

    }

    @Override
    public StockTrade deserialize(final String topic, final byte[] data) {
        try {
            return (StockTrade) BatchUtil.bytesToSerializableObject(data, Thread.currentThread().getContextClassLoader());
        } catch (final Exception e) {
            throw new BatchRuntimeException("Failed to deserialize data to StockTrade from topic " + topic, e);
        }
    }

    @Override
    public void close() {

    }
}
