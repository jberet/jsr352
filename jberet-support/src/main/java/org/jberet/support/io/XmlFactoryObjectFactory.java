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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.naming.spi.ObjectFactory} that produces instance of
 * {@code com.fasterxml.jackson.dataformat.xml.XmlFactory}. This class can be used to create a custom JNDI resource
 * in an application server. See wildfly.home/docs/schema/jboss-as-naming_2_0.xsd for more details.
 */
public final class XmlFactoryObjectFactory implements ObjectFactory {
    /**
     * Gets an instance of {@code com.fasterxml.jackson.dataformat.xml.XmlFactory} based on the resource configuration in the
     * application server. The parameter {@code environment} contains XmlFactory configuration properties, and accepts
     * the following properties:
     * <ul>
     * <li>inputDecorator: fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.InputDecorator}</li>
     * <li>outputDecorator: fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.OutputDecorator}</li>
     * <li>xmlTextElementName: </li>
     * <li>defaultUseWrapper:</li>
     * </ul>
     *
     * @param obj         the JNDI name of {@code com.fasterxml.jackson.dataformat.xml.XmlFactory} resource
     * @param name        always null
     * @param nameCtx     always null
     * @param environment a {@code Hashtable} of configuration properties
     * @return an instance of {@code com.fasterxml.jackson.dataformat.xml.XmlFactory}
     * @throws Exception any exception occurred
     */
    @Override
    public Object getObjectInstance(final Object obj,
                                    final Name name,
                                    final Context nameCtx,
                                    final Hashtable<?, ?> environment) throws Exception {
        final XmlFactory xmlFactory = new XmlFactory();
        JacksonXmlModule xmlModule = null;
        JsonFactoryObjectFactory.configureInputDecoratorAndOutputDecorator(xmlFactory, environment);

        final Object xmlTextElementName = environment.get("xmlTextElementName");
        if (xmlTextElementName != null) {
            xmlModule = new JacksonXmlModule();
            xmlModule.setXMLTextElementName((String) xmlTextElementName);
        }

        final Object defaultUseWrapper = environment.get("defaultUseWrapper");
        if (defaultUseWrapper != null) {
            if (defaultUseWrapper.equals("false")) {
                if (xmlModule == null) {
                    xmlModule = new JacksonXmlModule();
                }
                xmlModule.setDefaultUseWrapper(false);
            } else if (defaultUseWrapper.equals("true")) {
                //default value is already true, so nothing to do
            } else {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, (String) defaultUseWrapper, "defaultUseWrapper");
            }
        }

        final XmlMapper xmlMapper = xmlModule == null ? new XmlMapper(xmlFactory) : new XmlMapper(xmlFactory, xmlModule);
        xmlFactory.setCodec(xmlMapper);
        return xmlFactory;
    }
}
