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
import com.fasterxml.jackson.databind.MappingJsonFactory;
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

    protected JsonGenerator jsonGenerator;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        SupportLogger.LOGGER.tracef("Open JsonItemWriter with checkpoint %s, which is ignored for JsonItemWriter.%n", checkpoint);
        jsonFactory = new MappingJsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true);
        jsonGenerator = jsonFactory.createGenerator(getOutputWriter(writeMode, stepContext));
        jsonGenerator.useDefaultPrettyPrinter();
        if (!skipWritingHeader) {
            jsonGenerator.writeStartArray();
        }
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
            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
            if (resource.equalsIgnoreCase(RESOURCE_STEP_CONTEXT)) {
                final Object transientUserData = stepContext.getTransientUserData();
                if (OVERWRITE.equalsIgnoreCase(writeMode) || transientUserData == null) {
                    stepContext.setTransientUserData(stringWriter.toString());
                } else {
                    stepContext.setTransientUserData(transientUserData  +
                            stringWriter.toString());
                }
                stringWriter = null;
            }
            jsonGenerator.close();
            jsonGenerator = null;
        }
    }
}
