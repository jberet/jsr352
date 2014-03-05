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
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.io.OutputDecorator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes a list of same-typed objects to Json resource.
 * Each object is written as part of the root Json array.
 */
@Named
@Dependent
public class JsonItemWriter extends JsonItemReaderWriterBase implements ItemWriter {
    @Inject
    @BatchProperty
    protected String writeMode;

    @Inject
    @BatchProperty
    protected Map<String, String> jsonGeneratorFeatures;

    @Inject
    @BatchProperty
    protected String serializationFeatures;

    @Inject
    @BatchProperty
    protected Class prettyPrinter;

    @Inject
    @BatchProperty
    protected Class outputDecorator;

    @Inject
    @BatchProperty
    protected String customSerializers;

    protected JsonGenerator jsonGenerator;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        SupportLogger.LOGGER.tracef("Open JsonItemWriter with checkpoint %s, which is ignored for JsonItemWriter.%n", checkpoint);
        super.initJsonFactoryAndObjectMapper();

        if (outputDecorator != null) {
            jsonFactory.setOutputDecorator((OutputDecorator) outputDecorator.newInstance());
        }

        if (serializationFeatures != null) {
            MappingJsonFactoryObjectFactory.configureSerializationFeatures(objectMapper, serializationFeatures);
        }

        registerModule();

        jsonGenerator = jsonFactory.createGenerator(getOutputStream(writeMode));
        SupportLogger.LOGGER.openingResource(resource, this.getClass());

        if (jsonGeneratorFeatures != null) {
            for (final Map.Entry<String, String> e : jsonGeneratorFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final JsonGenerator.Feature feature;
                try {
                    feature = JsonGenerator.Feature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
                }
                if ("true".equals(value)) {
                    if (!feature.enabledByDefault()) {
                        jsonGenerator.configure(feature, true);
                    }
                } else if ("false".equals(value)) {
                    if (feature.enabledByDefault()) {
                        jsonGenerator.configure(feature, false);
                    }
                } else {
                    throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
                }
            }
        }

        if (prettyPrinter == null) {
            jsonGenerator.useDefaultPrettyPrinter();
        } else {
            jsonGenerator.setPrettyPrinter((PrettyPrinter) prettyPrinter.newInstance());
        }

        //write { regardless of the value of skipWritingHeader, since any existing content already ends with }
        jsonGenerator.writeStartArray();
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        for (final Object o : items) {
            jsonGenerator.writeObject(o);
        }
        jsonGenerator.flush();
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {
        if (jsonGenerator != null) {
            SupportLogger.LOGGER.closingResource(resource, this.getClass());
            jsonGenerator.close();
            jsonGenerator = null;
        }
    }

    @Override
    protected void registerModule() throws Exception {
        MappingJsonFactoryObjectFactory.configureCustomSerializersAndDeserializers(
                objectMapper, customSerializers, null, getClass().getClassLoader());
    }
}
