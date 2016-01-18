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

import java.io.IOException;
import java.util.Map;
import javax.batch.operations.BatchRuntimeException;

import org.apache.kafka.common.serialization.Serializer;
import org.jberet.util.BatchUtil;

public class StockTradeSerializer implements Serializer<StockTrade> {
    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {

    }

    @Override
    public byte[] serialize(final String topic, final StockTrade stockTrade) {
        try {
            return BatchUtil.objectToBytes(stockTrade);
        } catch (IOException e) {
            throw new BatchRuntimeException("Failed to serialize to topic " + topic + ", StockTrade: " + stockTrade, e);
        }
    }

    @Override
    public void close() {

    }
}
