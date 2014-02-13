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
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
    protected Map<String, String> jsonParserFeatures;

    @Inject
    @BatchProperty
    protected Map<String, String> deserializationFeatures;

    @Inject
    @BatchProperty
    protected Class inputDecorator;

    @Inject
    @BatchProperty
    protected Class[] customDeserializers;

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
        super.initJsonFactory();
        if (inputDecorator != null) {
            jsonFactory.setInputDecorator((InputDecorator) inputDecorator.newInstance());
        }

        if (deserializationFeatures != null) {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper(jsonFactory);
            }
            for (final Map.Entry<String, String> e : deserializationFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final DeserializationFeature feature;
                try {
                    feature = DeserializationFeature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
                }
                if ("true".equals(value)) {
                    if (!feature.enabledByDefault()) {
                        objectMapper.configure(feature, true);
                    }
                } else if ("false".equals(value)) {
                    if (feature.enabledByDefault()) {
                        objectMapper.configure(feature, false);
                    }
                } else {
                    throw SupportLogger.LOGGER.invalidReaderWriterProperty(value, key);
                }
            }
        }

        registerModule();

        jsonParser = jsonFactory.createParser(getInputReader(false));
        if (objectMapper != null) {
            jsonParser.setCodec(objectMapper);
        }

        if (jsonParserFeatures != null) {
            for (final Map.Entry<String, String> e : jsonParserFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final JsonParser.Feature feature;
                try {
                    feature = JsonParser.Feature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
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
                    throw SupportLogger.LOGGER.invalidReaderWriterProperty(value, key);
                }
            }
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

    @Override
    protected void registerModule() throws Exception {
        if (customDeserializers != null) {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper(jsonFactory);
            }
            final SimpleModule simpleModule = new SimpleModule("customDeserializer-module");
            for (final Class aClass : customDeserializers) {
                simpleModule.addDeserializer(aClass, (JsonDeserializer) aClass.newInstance());
            }
            objectMapper.registerModule(simpleModule);
        }
    }
}
