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

import com.fasterxml.jackson.core.JsonFactory;

/**
 * An implementation of {@code javax.naming.spi.ObjectFactory} that produces instance of
 * {@code com.fasterxml.jackson.core.JsonFactory}. The actual type of the produced object may be
 * {@code com.fasterxml.jackson.databind.MappingJsonFactory} (default), {@code com.fasterxml.jackson.core.JsonFactory},
 * or {@code com.fasterxml.jackson.dataformat.xml.XmlFactory}. This class can be used to create a custom JNDI resource
 * in an application server. See wildfly.home/docs/schema/jboss-as-naming_2_0.xsd for more details.
 */
public final class JsonFactoryObjectFactory implements ObjectFactory {
    /**
     * Gets an instance of {@code com.fasterxml.jackson.core.JsonFactory} based on the resource configuration in the application server.
     *
     * @param obj         the JNDI name of {@code com.fasterxml.jackson.core.JsonFactory} resource
     * @param name        always null
     * @param nameCtx     always null
     * @param environment a {@code Hashtable} of configuration properties for {@code com.fasterxml.jackson.core.JsonFactory}
     * @return an instance of {@code com.fasterxml.jackson.core.JsonFactory}
     * @throws Exception any exception occurred
     */
    @Override
    public Object getObjectInstance(final Object obj,
                                    final Name name,
                                    final Context nameCtx,
                                    final Hashtable<?, ?> environment) throws Exception {
        JsonFactory jsonFactory = null;

        return jsonFactory;
    }
}
