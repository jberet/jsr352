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
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads a collection of data from XML resource.
 * Users of this class should provide a bean class that represents individual data item in the source XML, and the
 * {@link #readItem()} method reads one item at a time and binds it to the provided bean type.
 *
 * @see XmlItemWriter
 * @see XmlItemReaderWriterBase
 * @since 1.0.2
 */
@Named
@Dependent
public class XmlItemReader extends XmlItemReaderWriterBase implements ItemReader {
    /**
     * The bean class that represents individual data item in the {@link #resource} XML, and the {@link #readItem()}
     * method reads one item at a time and binds it to the provided bean type. Required property. For example,
     * <p>
     * <ul>
     * <li>{@code org.jberet.support.io.StockTrade}
     * <li>{@code org.jberet.support.io.Person}
     * <li>{@code my.own.custom.ItemBean}
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * Specifies the start position (a positive integer starting from 1) to read the data. If reading from the beginning
     * of the input XML, there is no need to specify this property.
     */
    @Inject
    @BatchProperty
    protected int start;

    /**
     * Specify the end position in the data set (inclusive). Optional property, and defaults to {@code Integer.MAX_VALUE}.
     * If reading till the end of the input XML, there is no need to specify this property.
     */
    @Inject
    @BatchProperty
    protected int end;

    /**
     * Fully-qualified name of a class implementing {@code com.fasterxml.jackson.core.io.InputDecorator}, which
     * can be used to decorate input sources. Optional property, and defaults to null.
     *
     * @see "com.fasterxml.jackson.core.io.InputDecorator"
     * @see "com.fasterxml.jackson.core.JsonFactory#setInputDecorator(com.fasterxml.jackson.core.io.InputDecorator)"
     */
    @Inject
    @BatchProperty
    protected Class inputDecorator;

    /**
     * Alternate "virtual name" to use for XML CDATA segments; that is, text values.
     * Optional property and defaults to null (empty String, "", is used as the virtual name).
     *
     * @see "com.fasterxml.jackson.dataformat.xml.JacksonXmlModule#setXMLTextElementName(java.lang.String)"
     */
    @Inject
    @BatchProperty
    protected String xmlTextElementName;

    private FromXmlParser fromXmlParser;
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
            throw SupportMessages.MESSAGES.invalidStartPosition((Integer) checkpoint, start, end);
        }
        super.initXmlFactory();
        if (inputDecorator != null) {
            xmlFactory.setInputDecorator((InputDecorator) inputDecorator.newInstance());
        }

        fromXmlParser = (FromXmlParser) xmlFactory.createParser(getInputStream(resource, false));
        SupportLogger.LOGGER.openingResource(resource, this.getClass());
        token = fromXmlParser.nextToken();
    }

    @Override
    public Object readItem() throws Exception {
        if (rowNumber >= end) {
            return null;
        }
        int nestedObjectLevel = 0;
        do {
            token = fromXmlParser.nextToken();
            if (token == null) {
                return null;
            } else if (token == JsonToken.START_OBJECT) {
                nestedObjectLevel++;
                if (nestedObjectLevel == 1) {
                    rowNumber++;
                } else if (nestedObjectLevel < 1) {
                    throw SupportMessages.MESSAGES.unexpectedJsonContent(fromXmlParser.getCurrentLocation());
                }
                if (rowNumber >= start) {
                    break;
                }
            } else if (token == JsonToken.END_OBJECT) {
                nestedObjectLevel--;
            }
        } while (true);
        final Object readValue = xmlMapper.readValue(fromXmlParser, beanType);
        if (!skipBeanValidation) {
            ItemReaderWriterBase.validate(readValue);
        }
        return readValue;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return rowNumber;
    }

    @Override
    public void close() throws Exception {
        if (fromXmlParser != null) {
            SupportLogger.LOGGER.closingResource(resource, this.getClass());
            fromXmlParser.close();
            fromXmlParser = null;
        }
    }

    @Override
    protected void initXmlModule() {
        if (xmlTextElementName != null && !xmlTextElementName.isEmpty()) {
            xmlModule = new JacksonXmlModule();
            xmlModule.setXMLTextElementName(xmlTextElementName);
        }
    }
}
