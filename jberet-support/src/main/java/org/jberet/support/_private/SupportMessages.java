/*
 * Copyright (c) 2014-2018 Red Hat, Inc. and/or its affiliates.
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

import java.util.Collection;
import javax.batch.operations.BatchRuntimeException;

import com.fasterxml.jackson.core.JsonLocation;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 60000, max = 60499)
public interface SupportMessages {
    SupportMessages MESSAGES = Messages.getBundle(SupportMessages.class);

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


    @Message(id = 60014, value = "Failed to parse string %s to enum %s in CsvContext %s for CellProcessor %s")
    BatchRuntimeException failToParseEnum(@Cause Throwable throwable, Object val, String enumType, CsvContext context, CellProcessor cellProcessor);

    @Message(id = 60015, value = "Unrecognized reader or writer property %s = %s")
    BatchRuntimeException unrecognizedReaderWriterProperty(String key, String value);

    @Message(id = 60016, value = "Unexpected Json content near %s")
    BatchRuntimeException unexpectedJsonContent(JsonLocation jsonLocation);

    @Message(id = 60017, value = "Incompatible Excel file format: %s where *.xlsx format is required")
    BatchRuntimeException incompatibleExcelFileFormat(String resource);

    @Message(id = 60018, value = "Failed to read Excel header from resource %s, sheet %s")
    BatchRuntimeException failToReadExcelHeader(String resource, String sheet);

    @Message(id = 60019, value = "Expecting data types: %s, but got %s, %s")
    BatchRuntimeException unexpectedDataType(String expectedTypes, String actualType, Object val);

    @Message(id = 60020, value = "Expecting JMS message types: %s, but got %s, %s")
    BatchRuntimeException unexpectedJmsMessageType(String expectedTypes, String actualType, Object val);

    @Message(id = 60021, value = "Failed to determine parameterNames due to a complex SQL was supplied. please specify parameterNames explicitly")
    BatchRuntimeException failedToDetermineParameterNames();

    @Message(id = 60022, value = "Invalid checkpoint data: %s")
    BatchRuntimeException invalidCheckpoint(Object data);

    @Message(id = 60023, value = "REST API invocation failed with status code: %s, reason: %s, and entity: %s")
    BatchRuntimeException restApiFailure(int statusCode, String reason, Object responseEntity);

    @Message(id = 60024, value = "Failed to look up resource by name: %s")
    BatchRuntimeException failToLookup(@Cause Throwable throwable, String lookupName);

    @Message(id = 60025, value = "Process has exited with an error code %d. %s")
    String processExecutionFailure(int exitCode, final Collection<String> cmd);

    @Message(id = 60026, value = "Directory %s is invalid.")
    BatchRuntimeException invalidDirectory(String dir);

    @Message(id = 60027, value = "Expecting data type %s, but the actual data type is %s")
    BatchRuntimeException incompatibleDataType(String expectedType, String actualType);

}