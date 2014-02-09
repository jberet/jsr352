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
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.jberet.support._private.SupportLogger;

@Named
public class JsonItemReader extends JsonItemReaderWriterBase implements ItemReader {
    @Inject
    @BatchProperty
    protected int start;

    @Inject
    @BatchProperty
    protected int end;

    private JsonParser jsonParser;
    private JsonToken token;
    private int rowNumber;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (end == 0) {
            end = Integer.MAX_VALUE;
        }
        if (checkpoint != null) {
            start = (Integer) checkpoint;
        }
        if (start > end) {
            throw SupportLogger.LOGGER.invalidStartPosition((Integer) checkpoint, start, end);
        }

        jsonFactory = new MappingJsonFactory();
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        jsonParser = jsonFactory.createParser(getInputReader(false));
    }

    @Override
    public Object readItem() throws Exception {
        if (rowNumber >= end) {
            return null;
        }

        do {
            token = jsonParser.nextToken();
            if (token == null) {
                return null;
            } else if (token == JsonToken.START_OBJECT) {
                rowNumber++;
                if (rowNumber >= start) {
                    break;
                }
            }
        } while (true);

        final Object result = jsonParser.readValueAs(beanType);
        return result;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return rowNumber;
    }

    @Override
    public void close() throws Exception {
        if (jsonParser != null) {
            jsonParser.close();
            jsonParser = null;
        }
    }
}
