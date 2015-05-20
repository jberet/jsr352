/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
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

import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.io.OutputDecorator;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes a list of same-typed objects to Json resource.
 * Each object is written as part of the root Json array.
 *
 * @see JsonItemReader
 * @see JsonItemReaderWriterBase
 * @since 1.0.2
 */
@Named
@Dependent
public class JsonItemWriter extends JsonItemReaderWriterBase implements ItemWriter {
    /**
     * Instructs this class, when the target Json resource already exists, whether to append to, or overwrite
     * the existing resource, or fail. Valid values are {@code append}, {@code overwrite}, and {@code failIfExists}.
     * Optional property, and defaults to {@code append}.
     */
    @Inject
    @BatchProperty
    protected String writeMode;

    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.core.JsonGenerator} features.
     * Optional property and defaults to null. Keys and values must be defined in
     * {@code com.fasterxml.jackson.core.JsonGenerator.Feature}. For example,
     * <p/>
     * <pre>
     * WRITE_BIGDECIMAL_AS_PLAIN=true, WRITE_NUMBERS_AS_STRINGS=true, QUOTE_NON_NUMERIC_NUMBERS=false
     * </pre>
     *
     * @see "com.fasterxml.jackson.core.JsonGenerator.Feature"
     */
    @Inject
    @BatchProperty
    protected Map<String, String> jsonGeneratorFeatures;

    /**
     * Fully-qualified name of a class that implements {@code com.fasterxml.jackson.core.PrettyPrinter}, which
     * implements pretty printer functionality, such as indentation. Optional property and defaults to null (the
     * system default pretty printer is used). For example,
     * <p/>
     * <pre>
     * com.fasterxml.jackson.core.util.MinimalPrettyPrinter
     * </pre>
     *
     * @see "com.fasterxml.jackson.core.PrettyPrinter"
     * @see "com.fasterxml.jackson.core.JsonGenerator#setPrettyPrinter(com.fasterxml.jackson.core.PrettyPrinter)"
     */
    @Inject
    @BatchProperty
    protected Class prettyPrinter;

    /**
     * Fully-qualified name of a class that implements {@code com.fasterxml.jackson.core.io.OutputDecorator}, which
     * can be used to decorate output destinations. Typical use is to use a filter abstraction (filtered output stream,
     * writer) around original output destination, and apply additional processing during write operations.
     * Optional property and defaults to null. For example,
     * <p/>
     * <pre>
     * org.jberet.support.io.JsonItemReaderTest$NoopOutputDecorator
     * </pre>
     *
     * @see "com.fasterxml.jackson.core.io.OutputDecorator"
     * @see "com.fasterxml.jackson.core.JsonFactory#setOutputDecorator(com.fasterxml.jackson.core.io.OutputDecorator)"
     */
    @Inject
    @BatchProperty
    protected Class outputDecorator;

    protected JsonGenerator jsonGenerator;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        SupportLogger.LOGGER.tracef("Open JsonItemWriter with checkpoint %s, which is ignored for JsonItemWriter.%n", checkpoint);
        initJsonFactoryAndObjectMapper();

        jsonGenerator = configureJsonGenerator(jsonFactory, getOutputStream(writeMode), outputDecorator, jsonGeneratorFeatures);
        SupportLogger.LOGGER.openingResource(resource, this.getClass());

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

    protected static JsonGenerator configureJsonGenerator(final JsonFactory jsonFactory,
                                                          final OutputStream outputStream,
                                                          final Class<?> outputDecorator,
                                                          final Map<String, String> jsonGeneratorFeatures) throws Exception {
        if (outputDecorator != null) {
            jsonFactory.setOutputDecorator((OutputDecorator) outputDecorator.newInstance());
        }
        final JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream);

        if (jsonGeneratorFeatures != null) {
            for (final Map.Entry<String, String> e : jsonGeneratorFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final JsonGenerator.Feature feature;
                try {
                    feature = JsonGenerator.Feature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportMessages.MESSAGES.unrecognizedReaderWriterProperty(key, value);
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
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, value, key);
                }
            }
        }

        return jsonGenerator;
    }
}
