/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
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
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads data items from CSV files using jackson-dataformat-csv.
 *
 * @see CsvItemReader
 * @see JacksonCsvItemWriter
 * @see JacksonCsvItemReaderWriterBase
 * @since 1.2.0
 */
@Named
@Dependent
public class JacksonCsvItemReader extends JacksonCsvItemReaderWriterBase implements ItemReader {

    /**
     * Specifies the start position (a positive integer starting from 1) to read the data. If reading from the beginning
     * of the input CSV resource, there is no need to specify this property.
     */
    @Inject
    @BatchProperty
    protected int start;

    /**
     * Specify the end position in the data set (inclusive). Optional property, and defaults to {@code Integer.MAX_VALUE}.
     * If reading till the end of the input CSV resource, there is no need to specify this property.
     */
    @Inject
    @BatchProperty
    protected int end;

    /**
     * Whether the first data line (either first line of the document, if useHeader=false, or second, if useHeader=true)
     * should be completely ignored by parser. Needed to support CSV-like file formats that include additional
     * non-data content before real data begins (specifically some database dumps do this)
     * <p/>
     * Optional property, valid values are "true" and "false" and defaults to "false".
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected String skipFirstDataRow;

    /**
     * Character, if any, used to escape values. Most commonly defined as backslash ('\'). Only used by parser;
     * generator only uses quoting, including doubling up of quotes to indicate quote char itself.
     * Optional protected and defaults to null.
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected String escapeChar;

    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.core.JsonParser} features.
     * Optional property and defaults to null. For example,
     * <p/>
     * <pre>
     * ALLOW_COMMENTS=true, ALLOW_YAML_COMMENTS=true, ALLOW_NUMERIC_LEADING_ZEROS=true, STRICT_DUPLICATE_DETECTION=true
     * </pre>
     *
     * @see "com.fasterxml.jackson.core.JsonParser.Feature"
     */
    @Inject
    @BatchProperty
    protected Map<String, String> jsonParserFeatures;

    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.dataformat.csv.CsvParser.Feature}.
     * Optional property and defaults to null. For example,
     * <p/>
     * <pre>
     * TRIM_SPACES=false, WRAP_AS_ARRAY=false
     * </pre>
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvParser.Feature"
     */
    @Inject
    @BatchProperty
    protected Map<String, String> csvParserFeatures;

    /**
     * A comma-separated list of fully-qualified names of classes that implement
     * {@code com.fasterxml.jackson.databind.deser.DeserializationProblemHandler}, which can be registered to get
     * called when a potentially recoverable problem is encountered during deserialization process.
     * Handlers can try to resolve the problem, throw an exception or do nothing. Optional property and defaults to null.
     * For example,
     * <p/>
     * <pre>
     * org.jberet.support.io.JsonItemReaderTest$UnknownHandler, org.jberet.support.io.JsonItemReaderTest$UnknownHandler2
     * </pre>
     *
     * @see "com.fasterxml.jackson.databind.deser.DeserializationProblemHandler"
     * @see "com.fasterxml.jackson.databind.ObjectMapper#addHandler(com.fasterxml.jackson.databind.deser.DeserializationProblemHandler)"
     * @see MappingJsonFactoryObjectFactory#configureDeserializationProblemHandlers(com.fasterxml.jackson.databind.ObjectMapper, java.lang.String, java.lang.ClassLoader)
     */
    @Inject
    @BatchProperty
    protected String deserializationProblemHandlers;

    /**
     * Fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.InputDecorator}, which can be
     * used to decorate input sources. Typical use is to use a filter abstraction (filtered stream, reader)
     * around original input source, and apply additional processing during read operations. Optional property and
     * defaults to null. For example,
     * <p/>
     * <pre>
     * org.jberet.support.io.JsonItemReaderTest$NoopInputDecorator
     * </pre>
     *
     * @see "com.fasterxml.jackson.core.JsonFactory#setInputDecorator(com.fasterxml.jackson.core.io.InputDecorator)"
     * @see "com.fasterxml.jackson.core.io.InputDecorator"
     */
    @Inject
    @BatchProperty
    protected Class inputDecorator;

    private CsvParser csvParser;
    private int rowNumber;
    private boolean rawAccess;

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
        init();
        csvParser = (CsvParser) JsonItemReader.configureJsonParser(this, inputDecorator, deserializationProblemHandlers,
                jsonParserFeatures);

        if (csvParserFeatures != null) {
            for (final Map.Entry<String, String> e : csvParserFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final CsvParser.Feature feature;
                try {
                    feature = CsvParser.Feature.valueOf(key);
                } catch (final Exception e1) {
                    throw SupportMessages.MESSAGES.unrecognizedReaderWriterProperty(key, value);
                }
                if ("true".equals(value)) {
                    if (!feature.enabledByDefault()) {
                        csvParser.configure(feature, true);
                    }
                } else if ("false".equals(value)) {
                    if (feature.enabledByDefault()) {
                        csvParser.configure(feature, false);
                    }
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, value, key);
                }
            }
        }

        rawAccess = beanType == List.class || beanType == String[].class;
        if (!rawAccess) {
            CsvSchema schema;
            if (columns != null) {
                schema = buildCsvSchema(null);
            } else {
                //columns not defined, but beanType is either Map.class or Pojo.class or JsonNode.class
                if (useHeader) {
                    schema = buildCsvSchema(CsvSchema.emptySchema());
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, columns, "columns");
                }
            }

            if (escapeChar != null) {
                schema = schema.withEscapeChar(escapeChar.charAt(0));
            }
            if (skipFirstDataRow != null) {
                schema = schema.withSkipFirstDataRow(Boolean.parseBoolean(skipFirstDataRow.trim()));
            }
            csvParser.setSchema(schema);
        }
    }

    @Override
    public void close() throws Exception {
        if (csvParser != null) {
            SupportLogger.LOGGER.closingResource(resource, this.getClass());
            if (deserializationProblemHandlers != null) {
                objectMapper.clearProblemHandlers();
            }
            csvParser.close();
            csvParser = null;
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (rowNumber >= end) {
            return null;
        }

        JsonToken token;
        final Object readValue;

        if (!rawAccess) {
            do {
                token = csvParser.nextToken();
                if (token == null) {
                    return null;
                }
                if (token == JsonToken.START_OBJECT) {
                    if (++rowNumber >= start) {
                        break;
                    }
                }
            } while (true);

            readValue = objectMapper.readValue(csvParser, beanType);
            if (!skipBeanValidation) {
                ItemReaderWriterBase.validate(readValue);
            }
        } else {
            do {
                token = csvParser.nextToken();
                if (token == null) {
                    return null;
                }
                if (token == JsonToken.START_ARRAY) {
                    if (++rowNumber >= start) {
                        break;
                    }
                }
            } while (true);

            readValue = objectMapper.readValue(csvParser, beanType);
        }
        return readValue;
    }

    /**
     * Gets the current row number in the {@code ResultSet} as the checkpoint info.
     *
     * @return the current row number in the {@code ResultSet}
     * @throws Exception any exception raised
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        return rowNumber;
    }
}
