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

package org.jberet.support._private;

import java.io.File;
import javax.batch.operations.BatchRuntimeException;

import com.fasterxml.jackson.core.JsonLocation;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 60000, max = 60999)
public interface SupportLogger extends BasicLogger {
    SupportLogger LOGGER = Logger.getMessageLogger(SupportLogger.class, "org.jberet.support");

    @Message(id = 60000, value = "Invalid reader or writer property value %s for key %s")
    BatchRuntimeException invalidReaderWriterProperty(@Cause Throwable th, String val, String key);

    @Message(id = 60001, value = "Failed to read header from CSV resource %s")
    BatchRuntimeException failToReadCsvHeader(@Cause Throwable th, String csvResource);

    @Message(id = 60002, value = "Failed to load or instantiate custom type based on property value %s")
    BatchRuntimeException failToLoadOrCreateCustomType(@Cause Throwable th, String contextStringVal);

    @Message(id = 60003, value = "Invalid property value format (missing quote): %s")
    BatchRuntimeException missingQuote(String propertyVal);

    @Message(id = 60004, value = "Failed to open stream from resource: %s")
    BatchRuntimeException failToOpenStream(@Cause Throwable throwable, String resource);

    @Message(id = 60005, value = "Invalid position %s to start reading, the configured range is between %s and %s")
    BatchRuntimeException invalidStartPosition(int startPosition, int start, int end);

    @Message(id = 60007, value = "Unexpected character %s at position %s in character array %s")
    BatchRuntimeException unexpectedChar(char ch, int position, String chars);

    @Message(id = 60008, value = "Invalid parameters for CellProcessor: %s %s")
    BatchRuntimeException invalidParamsForCellProcessor(String cellProcessorName, String[] params);

    @Message(id = 60009, value = "Unsupported CellProcessor: %s %s")
    BatchRuntimeException unsupportedCellProcessor(String cellProcessorName, String[] params);

    @Message(id = 60010, value = "The target writer resource already exists: %s")
    BatchRuntimeException writerResourceAlreadyExists(Object writerResource);

    @Message(id = 60011, value = "The target writer resource is a directory: %s")
    BatchRuntimeException writerResourceIsDirectory(File file);

    @Message(id = 60013, value = "The CellProcessor value may be missing an ending single quote: %s")
    @LogMessage(level = Logger.Level.WARN)
    void maybeMissingEndQuote(String line);

    @Message(id = 60014, value = "Failed to parse string %s to enum %s in CsvContext %s for CellProcessor %s")
    BatchRuntimeException failToParseEnum(@Cause Throwable throwable, Object val, String enumType, CsvContext context, CellProcessor cellProcessor);

    @Message(id = 60015, value = "Unrecognized reader or writer property %s = %s")
    BatchRuntimeException unrecognizedReaderWriterProperty(String key, String value);

    @Message(id = 60016, value = "Unexpected Json content near %s")
    BatchRuntimeException unexpectedJsonContent(JsonLocation jsonLocation);

    @Message(id = 60017, value = "Opening resource %s in %s")
    @LogMessage(level = Logger.Level.INFO)
    void openingResource(String resource, Class<?> cls);

    @Message(id = 60018, value = "Closing resource %s in %s")
    @LogMessage(level = Logger.Level.INFO)
    void closingResource(String resource, Class<?> cls);

}