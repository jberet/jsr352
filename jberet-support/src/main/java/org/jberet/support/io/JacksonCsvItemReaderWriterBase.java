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

import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.naming.InitialContext;

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * The base class for {@link JacksonCsvItemReader} and {@link JacksonCsvItemWriter}.
 *
 * @see JacksonCsvItemReader
 * @see JacksonCsvItemWriter
 * @since 1.1.0
 */
public abstract class JacksonCsvItemReaderWriterBase extends JsonItemReaderWriterBase {
    /**
     * For {@code ItemReader}, it's the java type that each data item should be converted to; for {@code ItemWriter},
     * it's the java type for each incoming data item. In either case, the valid values are:
     * <p/>
     * <ul>
     * <li>a custom java type that represents data item and serves as the CSV schema class
     * <li>{@code java.util.Map}
     * <li>{@code java.util.List}
     * <li>{@code java.lang.String[]}
     * <li>{@code com.fasterxml.jackson.databind.JsonNode}
     * </ul>
     * <p>
     * When using {@code java.util.List} or {@code java.lang.String[]} for reading, it is deemed raw access, and CSV
     * schema will not be configured and any schema-related properties are ignored. Specifically, CSV header and
     * comment lines are read as raw access content.
     */
    @Inject
    @BatchProperty
    protected Class beanType;
    /**
     * Specifies CSV schema in one of the 2 ways:
     * <p/>
     * <ul>
     * <li>columns = "&lt;fully-qualified class name&gt;":<br>
     * CSV schema is defined in the named POJO class, which typically has class-level annotation
     * {@code com.fasterxml.jackson.annotation.JsonPropertyOrder} to define property order corresponding to
     * CSV column order.</li>
     * <p/>
     * <li>columns = "&lt;comma-separated list of column names, each of which may be followed by a space and column type":<br>
     * use the value to manually build CSV schema. Valid column types are defined in
     * {@code com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType}, including:
     * <ul>
     * <li>STRING
     * <li>STRING_OR_LITERAL
     * <li>NUMBER
     * <li>NUMBER_OR_STRING
     * <li>BOOLEAN
     * <li>ARRAY
     * </ul>
     * For complete list and descriptioin, see {@code com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType} javadoc.
     * </li>
     * </ul>
     * <p/>
     * For example,
     * <pre>
     * columns = "org.jberet.support.io.StockTrade"
     * columns = "firstName STRING, lastName STRING, age NUMBER"
     * </pre>
     * In {@link JacksonCsvItemReader}, if this property is not defined and {@link #useHeader} is true
     * (CSV input has a header), the header is used to create CSV schema. However, when {@link #beanType} is
     * {@code java.util.List} or {@code java.lang.String[]}, the reader is considered raw access, and all schema-related
     * properties are ignored.
     * <p/>
     * This property is optional for reader and required for writer class.
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected String columns;

    /**
     * whether the first line of physical document defines column names (true) or not (false):
     * if enabled, parser will take first-line values to define column names; and generator will output column names as
     * the first line Optional property.
     * <p>
     * For {@link JacksonCsvItemReader}, if {@link #beanType} is {@code java.util.List} or {@code java.lang.String[]},
     * it is considered raw access, {@code useHeader} property is ignored and no CSV schema is used.
     * <p>
     * valid values are {@code true} or {@code false}, and the default is {@code false}.
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected boolean useHeader;

    /**
     * Character used for quoting values that contain quote characters or linefeeds.
     * Optional property and defaults to " (double-quote character).
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected String quoteChar;

    /**
     * Character used to separate values.
     * <p/>
     * Optional property and defaults to , (comma character).
     * Other commonly used values include tab ('\t') and pipe ('|')
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected String columnSeparator;

    /**
     * When asked to write Java `null`, this String value will be used instead.
     * Optional property and defaults to empty string.
     *
     * @see "com.fasterxml.jackson.dataformat.csv.CsvSchema"
     */
    @Inject
    @BatchProperty
    protected String nullValue;


    protected CsvMapper csvMapper;

    protected void init() throws Exception {
        initJsonFactoryAndObjectMapper();
        csvMapper = (CsvMapper) objectMapper;
    }

    @Override
    protected void initJsonFactory() throws Exception {
        if (jsonFactoryLookup != null) {
            jsonFactory = InitialContext.doLookup(jsonFactoryLookup);
        } else {
            jsonFactory = new CsvFactory(new CsvMapper());
        }
    }

    protected CsvSchema buildCsvSchema(CsvSchema schema) throws Exception {
        if (schema == null) {
            columns = columns.trim();
            if (columns.indexOf(',') < 0 && columns.indexOf(' ') < 0) {
                //no comma and no space, assume it's java class name for schema
                schema = csvMapper.schemaFor(getClass().getClassLoader().loadClass(columns));
            } else {
                //manually build CsvSchema
                final String[] cols = columns.split(",");
                final CsvSchema.Builder builder = new CsvSchema.Builder();
                for (String e : cols) {
                    e = e.trim();
                    final int lastSpace = e.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        final String e1 = e.substring(0, lastSpace).trim();
                        final String e2 = e.substring(lastSpace + 1);
                        builder.addColumn(e1, CsvSchema.ColumnType.valueOf(e2));
                    } else {
                        builder.addColumn(e);
                    }
                }
                schema = builder.build();
            }
        }

        schema = useHeader ? schema.withHeader() : schema.withoutHeader();

        if (columnSeparator != null) {
            schema = schema.withColumnSeparator(columnSeparator.charAt(0));
        }
        if (quoteChar != null) {
            schema = schema.withQuoteChar(quoteChar.charAt(0));
        }
        if (nullValue != null) {
            schema = schema.withNullValue(nullValue);
        }

        //to allow comments like "# this is comments".
        //comments can be enabled or disabled with com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_YAML_COMMENTS
        //which corresponds to batch property jsonParserFeatures
        return schema.withComments();
    }
}
