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
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.io.OutputDecorator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes a list of same-typed objects to XML resource.
 * Each object is written as a sub-element of the target XML resource. The XML root element is specified with the
 * injected batch artifact properties {@link #rootElementName}, {@link #rootElementNamespaceURI}, and
 * {@link #rootElementPrefix}.
 */
@Named
@Dependent
public class XmlItemWriter extends XmlItemReaderWriterBase implements ItemWriter {
    @Inject
    @BatchProperty
    protected String writeMode;

    @Inject
    @BatchProperty
    protected String defaultUseWrapper;

    @Inject
    @BatchProperty
    protected String rootElementName;

    @Inject
    @BatchProperty
    protected String rootElementPrefix;

    @Inject
    @BatchProperty
    protected String rootElementNamespaceURI;

    @Inject
    @BatchProperty
    protected Class prettyPrinter;

    @Inject
    @BatchProperty
    protected Class outputDecorator;

    protected ToXmlGenerator toXmlGenerator;
    private XMLStreamWriter staxWriter;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        SupportLogger.LOGGER.tracef("Open XmlItemWriter with checkpoint %s, which is ignored for XmlItemWriter.%n", checkpoint);
        super.initXmlFactory();

        if (outputDecorator != null) {
            xmlFactory.setOutputDecorator((OutputDecorator) outputDecorator.newInstance());
        }
        xmlMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);

        toXmlGenerator = xmlFactory.createGenerator(getOutputStream(writeMode));
        SupportLogger.LOGGER.openingResource(resource, this.getClass());

        if (prettyPrinter == null) {
            toXmlGenerator.useDefaultPrettyPrinter();
        } else {
            toXmlGenerator.setPrettyPrinter((PrettyPrinter) prettyPrinter.newInstance());
        }

        staxWriter = toXmlGenerator.getStaxWriter();
        if (!this.skipWritingHeader) {
            staxWriter.writeStartDocument();
        }
        staxWriter.writeCharacters(NEW_LINE);
        if (rootElementName == null || rootElementName.isEmpty()) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, rootElementName, "rootElementName");
        }
        if (rootElementPrefix == null || rootElementPrefix.isEmpty()) {
            if (rootElementNamespaceURI == null || rootElementNamespaceURI.isEmpty()) {
                staxWriter.writeStartElement(rootElementName);
            } else {
                staxWriter.writeStartElement(rootElementNamespaceURI, rootElementName);
            }
        } else {
            if (rootElementNamespaceURI == null || rootElementNamespaceURI.isEmpty()) {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, rootElementNamespaceURI, "rootElementNamespaceURI");
            } else {
                staxWriter.writeStartElement(rootElementPrefix, rootElementName, rootElementNamespaceURI);
            }
        }
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        for (final Object o : items) {
            staxWriter.writeCharacters(NEW_LINE);
            toXmlGenerator.writeObject(o);

        }
        toXmlGenerator.flush();
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {
        if (toXmlGenerator != null) {
            SupportLogger.LOGGER.closingResource(resource, this.getClass());
            staxWriter.writeCharacters(NEW_LINE);
            staxWriter.writeEndDocument();
            toXmlGenerator.close();
            toXmlGenerator = null;
        }
    }

    @Override
    protected void initXmlModule() {
        if (defaultUseWrapper != null) {
            if (defaultUseWrapper.equals("false")) {
                xmlModule = new JacksonXmlModule();
                xmlModule.setDefaultUseWrapper(false);
            } else if (defaultUseWrapper.equals("true")) {
                //default value is already true, so nothing to do
            } else {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, defaultUseWrapper, "defaultUseWrapper");
            }
        }
    }
}
