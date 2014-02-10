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
import org.jberet.support._private.SupportLogger;

@Named
public class JsonItemReader extends JsonItemReaderWriterBase implements ItemReader {
    @Inject
    @BatchProperty
    protected Class beanType;

    @Inject
    @BatchProperty
    protected int start;

    @Inject
    @BatchProperty
    protected int end;

    @Inject
    @BatchProperty
    protected String AUTO_CLOSE_SOURCE;

    @Inject
    @BatchProperty
    protected String ALLOW_COMMENTS;

    @Inject
    @BatchProperty
    protected String ALLOW_YAML_COMMENTS;

    @Inject
    @BatchProperty
    protected String ALLOW_UNQUOTED_FIELD_NAMES;

    @Inject
    @BatchProperty
    protected String ALLOW_SINGLE_QUOTES;

    @Inject
    @BatchProperty
    protected String ALLOW_UNQUOTED_CONTROL_CHARS;

    @Inject
    @BatchProperty
    protected String ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER;

    @Inject
    @BatchProperty
    protected String ALLOW_NUMERIC_LEADING_ZEROS;

    @Inject
    @BatchProperty
    protected String ALLOW_NON_NUMERIC_NUMBERS;

    @Inject
    @BatchProperty
    protected String STRICT_DUPLICATE_DETECTION;

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
        super.open(checkpoint);
        jsonParser = jsonFactory.createParser(getInputReader(false));

        if ("false".equals(AUTO_CLOSE_SOURCE)) {
            jsonParser.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        }
        if ("true".equals(ALLOW_COMMENTS)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        }
        if ("true".equals(ALLOW_YAML_COMMENTS)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
        }
        if ("true".equals(ALLOW_UNQUOTED_FIELD_NAMES)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        }
        if ("true".equals(ALLOW_SINGLE_QUOTES)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        }
        if ("true".equals(ALLOW_UNQUOTED_CONTROL_CHARS)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        }
        if ("true".equals(ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        }
        if ("true".equals(ALLOW_NUMERIC_LEADING_ZEROS)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        }
        if ("true".equals(ALLOW_NON_NUMERIC_NUMBERS)) {
            jsonParser.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        }
        if ("true".equals(STRICT_DUPLICATE_DETECTION)) {
            jsonParser.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
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
        return jsonParser.readValueAs(beanType);
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
