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

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Base class for {@link org.jberet.support.io.XmlItemReader} and {@link org.jberet.support.io.XmlItemWriter}.
 */
public abstract class XmlItemReaderWriterBase extends ItemReaderWriterBase {
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
    protected void initXmlFactory() {
        initXmlModule();
        xmlFactory = new XmlFactory();
        xmlMapper = new XmlMapper(xmlFactory, xmlModule);
        xmlFactory.setCodec(xmlMapper);
    }
}
