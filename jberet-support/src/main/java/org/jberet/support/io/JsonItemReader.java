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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jberet.support._private.SupportLogger;

@Named
public class JsonItemReader implements ItemReader {
    @Inject
    @BatchProperty
    protected String resource;

    @Inject
    @BatchProperty
    protected int start;

    @Inject
    @BatchProperty
    protected int end;

    @Inject
    @BatchProperty
    protected Class beanType;

    private JsonFactory jsonFactory;
    private JsonParser jsonParser;
    private ObjectMapper objectMapper;
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
        jsonParser = jsonFactory.createParser(ReaderWriterUtil.getInputReader(resource, false));
        objectMapper = new ObjectMapper(jsonFactory);
        if (checkpoint == null) {
            //token = jsonParser.nextToken();
        } else {

        }
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
        System.out.printf("token: %s, row %s%n%s%n", token, rowNumber, result);
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
        }
    }
}
