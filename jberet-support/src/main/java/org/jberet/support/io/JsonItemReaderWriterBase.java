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

import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jberet.support._private.SupportLogger;

/**
 * Base class for {@link org.jberet.support.io.JsonItemReader} and {@link org.jberet.support.io.JsonItemWriter}.
 * It also holds Json-related common batch properties for customizing Json processing.
 */
public abstract class JsonItemReaderWriterBase extends ItemReaderWriterBase {

    @Inject
    @BatchProperty
    protected Map<String, String> jsonFactoryFeatures;

    @Inject
    @BatchProperty
    protected Map<String, String> mapperFeatures;

    protected JsonFactory jsonFactory;
    protected ObjectMapper objectMapper;

    /**
     * Registers any {@code com.fasterxml.jackson.databind.module.SimpleModule} to the {@link #objectMapper}. Any number
     * of custom serializers or deserializers can be added to the module.
     */
    protected abstract void registerModule() throws Exception;

    /**
     * Initializes {@link #jsonFactory} and {@link #objectMapper} fields, which may be instantiated or obtained from
     * other part of the application. This method also configures the {@link #jsonFactory} and {@link #objectMapper}
     * properly based on the current batch artifact properties.
     */
    protected void initJsonFactoryAndObjectMapper() {
        jsonFactory = new MappingJsonFactory();
        objectMapper = (ObjectMapper) jsonFactory.getCodec();
        if (jsonFactoryFeatures != null) {
            for (final Map.Entry<String, String> e : jsonFactoryFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final JsonFactory.Feature feature;
                try {
                    feature = JsonFactory.Feature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
                }
                if ("true".equals(value)) {
                    if (!feature.enabledByDefault()) {
                        jsonFactory.configure(feature, true);
                    }
                } else if ("false".equals(value)) {
                    if (feature.enabledByDefault()) {
                        jsonFactory.configure(feature, false);
                    }
                } else {
                    throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
                }
            }
        }
        if (mapperFeatures != null) {
            for (final Map.Entry<String, String> e : mapperFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final MapperFeature feature;
                try {
                    feature = MapperFeature.valueOf(key);
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
                    throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
                }
            }
        }
    }
}
