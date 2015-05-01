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

import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.naming.InitialContext;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Base class for {@link org.jberet.support.io.XmlItemReader} and {@link org.jberet.support.io.XmlItemWriter}.
 *
 * @see XmlItemReader
 * @see XmlItemWriter
 * @since 1.0.2
 */
public abstract class XmlItemReaderWriterBase extends ItemReaderWriterBase {
    /**
     * JNDI lookup name for {@code com.fasterxml.jackson.dataformat.xml.XmlFactory}, which is used for constructing
     * {@link com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser} in {@link org.jberet.support.io.XmlItemReader} and
     * {@link com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator} in {@link org.jberet.support.io.XmlItemWriter}.
     * Optional property and defaults to null. When this property is specified, its value is used to look up an
     * instance of {@code com.fasterxml.jackson.dataformat.xml.XmlFactory}, which is typically created and
     * administrated externally (e.g., inside application server). Otherwise, a new instance of
     * {@code com.fasterxml.jackson.dataformat.xml.XmlFactory} is created instead of lookup.
     *
     * @see XmlFactoryObjectFactory
     */
    @Inject
    @BatchProperty
    protected String xmlFactoryLookup;

    /**
     * A comma-separated list of Jackson datatype module type ids that extend {@code com.fasterxml.jackson.databind.Module}.
     * These modules will be registered with {@link #xmlMapper}. For example,
     * <p/>
     * <pre>
     * com.fasterxml.jackson.datatype.joda.JodaModule, com.fasterxml.jackson.datatype.jsr353.JSR353Module, com.fasterxml.jackson.datatype.jsr310.JSR310Module
     * </pre>
     *
     * @see JsonItemReaderWriterBase#customDataTypeModules
     */
    @Inject
    @BatchProperty
    protected String customDataTypeModules;

    protected JacksonXmlModule xmlModule;
    protected XmlFactory xmlFactory;
    protected XmlMapper xmlMapper;

    /**
     * Initializes {@link #xmlModule}, if needed by job xml configuration.
     */
    protected abstract void initXmlModule();

    /**
     * Initializes {@link #xmlFactory} field, which may be instantiated or obtained from other part of the application.
     */
    protected void initXmlFactory() throws Exception {
        if (xmlFactoryLookup != null) {
            xmlFactory = InitialContext.doLookup(xmlFactoryLookup);
            xmlMapper = (XmlMapper) xmlFactory.getCodec();
        } else {
            initXmlModule();
            xmlFactory = new XmlFactory();
            xmlMapper = xmlModule == null ? new XmlMapper(xmlFactory) : new XmlMapper(xmlFactory, xmlModule);
            xmlFactory.setCodec(xmlMapper);
        }
        MappingJsonFactoryObjectFactory.configureCustomSerializersAndDeserializers(xmlMapper, null, null, customDataTypeModules, getClass().getClassLoader());
    }
}
