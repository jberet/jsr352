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
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.InputDecorator;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from Json resource that consists of a
 * collection of same-typed data items. Its {@link #readItem()} method reads one item at a time, and binds it to a
 * user-provided bean type that represents individual data item in the source Json resource. The data item may also
 * be bound to {@code java.util.Map} or {@code com.fasterxml.jackson.databind.JsonNode} for applications that do not
 * need application bean type.
 *
 * @see     JsonItemWriter
 * @see     JsonItemReaderWriterBase
 * @since   1.0.2
 */
@Named
@Dependent
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
    protected Map<String, String> jsonParserFeatures;

    @Inject
    @BatchProperty
    protected String deserializationProblemHandlers;

    @Inject
    @BatchProperty
    protected Class inputDecorator;

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
            throw SupportMessages.MESSAGES.invalidStartPosition((Integer) checkpoint, start, end);
        }
        initJsonFactoryAndObjectMapper();
        if (inputDecorator != null) {
            jsonFactory.setInputDecorator((InputDecorator) inputDecorator.newInstance());
        }

        jsonParser = jsonFactory.createParser(getInputStream(resource, false));

        if (deserializationProblemHandlers != null) {
            MappingJsonFactoryObjectFactory.configureDeserializationProblemHandlers(
                    objectMapper, deserializationProblemHandlers, getClass().getClassLoader());
        }
        SupportLogger.LOGGER.openingResource(resource, this.getClass());

        if (jsonParserFeatures != null) {
            for (final Map.Entry<String, String> e : jsonParserFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final JsonParser.Feature feature;
                try {
                    feature = JsonParser.Feature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportMessages.MESSAGES.unrecognizedReaderWriterProperty(key, value);
                }
                if ("true".equals(value)) {
                    if (!feature.enabledByDefault()) {
                        jsonParser.configure(feature, true);
                    }
                } else if ("false".equals(value)) {
                    if (feature.enabledByDefault()) {
                        jsonParser.configure(feature, false);
                    }
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, value, key);
                }
            }
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (rowNumber >= end) {
            return null;
        }
        int nestedObjectLevel = 0;
        do {
            token = jsonParser.nextToken();
            if (token == null) {
                return null;
            } else if (token == JsonToken.START_OBJECT) {
                nestedObjectLevel++;
                if (nestedObjectLevel == 1) {
                    rowNumber++;
                } else if (nestedObjectLevel < 1) {
                    throw SupportMessages.MESSAGES.unexpectedJsonContent(jsonParser.getCurrentLocation());
                }
                if (rowNumber >= start) {
                    break;
                }
            } else if (token == JsonToken.END_OBJECT) {
                nestedObjectLevel--;
            }
        } while (true);
        final Object readValue = objectMapper.readValue(jsonParser, beanType);
        if (!skipBeanValidation) {
            ItemReaderWriterBase.validate(readValue);
        }
        return readValue;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return rowNumber;
    }

    @Override
    public void close() throws Exception {
        if (jsonParser != null) {
            SupportLogger.LOGGER.closingResource(resource, this.getClass());
            if (deserializationProblemHandlers != null) {
                objectMapper.clearProblemHandlers();
            }
            jsonParser.close();
            jsonParser = null;
        }
    }
}
