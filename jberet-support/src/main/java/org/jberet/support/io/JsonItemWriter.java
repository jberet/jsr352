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
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonGenerator;
import org.jberet.support._private.SupportLogger;

import static org.jberet.support.io.CsvProperties.OVERWRITE;
import static org.jberet.support.io.CsvProperties.RESOURCE_STEP_CONTEXT;

@Named
public class JsonItemWriter extends JsonItemReaderWriterBase implements ItemWriter {
    @Inject
    protected StepContext stepContext;

    @Inject
    @BatchProperty
    protected String writeMode;

    @Inject
    @BatchProperty
    protected String AUTO_CLOSE_TARGET;

    @Inject
    @BatchProperty
    protected String AUTO_CLOSE_JSON_CONTENT;

    @Inject
    @BatchProperty
    protected String QUOTE_FIELD_NAMES;

    @Inject
    @BatchProperty
    protected String QUOTE_NON_NUMERIC_NUMBERS;

    @Inject
    @BatchProperty
    protected String WRITE_NUMBERS_AS_STRINGS;

    @Inject
    @BatchProperty
    protected String WRITE_BIGDECIMAL_AS_PLAIN;

    @Inject
    @BatchProperty
    protected String FLUSH_PASSED_TO_STREAM;

    @Inject
    @BatchProperty
    protected String ESCAPE_NON_ASCII;

    @Inject
    @BatchProperty
    protected String STRICT_DUPLICATE_DETECTION;

    protected JsonGenerator jsonGenerator;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        SupportLogger.LOGGER.tracef("Open JsonItemWriter with checkpoint %s, which is ignored for JsonItemWriter.%n", checkpoint);
        super.open(checkpoint);
        jsonGenerator = jsonFactory.createGenerator(getOutputWriter(writeMode, stepContext));

        if ("false".equals(AUTO_CLOSE_TARGET)) {
            jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        }
        if ("false".equals(AUTO_CLOSE_JSON_CONTENT)) {
            jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        }
        if ("false".equals(QUOTE_FIELD_NAMES)) {
            jsonGenerator.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
        }
        if ("false".equals(QUOTE_NON_NUMERIC_NUMBERS)) {
            jsonGenerator.configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, false);
        }
        if ("true".equals(WRITE_NUMBERS_AS_STRINGS)) {
            jsonGenerator.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        }
        if ("true".equals(WRITE_BIGDECIMAL_AS_PLAIN)) {
            jsonGenerator.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        }
        if ("false".equals(FLUSH_PASSED_TO_STREAM)) {
            jsonGenerator.configure(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, false);
        }
        if ("true".equals(ESCAPE_NON_ASCII)) {
            jsonGenerator.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        }
        if ("true".equals(STRICT_DUPLICATE_DETECTION)) {
            jsonGenerator.configure(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION, true);
        }

        jsonGenerator.useDefaultPrettyPrinter();
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
            jsonGenerator.close();
            jsonGenerator = null;
            if (resource.equalsIgnoreCase(RESOURCE_STEP_CONTEXT)) {
                final Object transientUserData = stepContext.getTransientUserData();
                if (OVERWRITE.equalsIgnoreCase(writeMode) || transientUserData == null) {
                    stepContext.setTransientUserData(stringWriter.toString());
                } else {
                    stepContext.setTransientUserData(transientUserData +
                            stringWriter.toString());
                }
                stringWriter = null;
            }
        }
    }
}
