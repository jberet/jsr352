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
import java.util.StringTokenizer;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.core.io.OutputDecorator;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.naming.spi.ObjectFactory} that produces instance of
 * {@code com.fasterxml.jackson.core.JsonFactory}. This class can be used to create a custom JNDI resource
 * in an application server. See wildfly.home/docs/schema/jboss-as-naming_2_0.xsd for more details.
 */
public final class JsonFactoryObjectFactory implements ObjectFactory {
    /**
     * Gets an instance of {@code com.fasterxml.jackson.core.JsonFactory} based on the resource configuration in the
     * application server. The parameter {@code environment} contains JsonFactory configuration properties, and accepts
     * the following properties:
     * <ul>
     * <li>jsonFactoryFeatures: JsonFactory features as defined in com.fasterxml.jackson.core.JsonFactory.Feature</li>
     * <li>inputDecorator: fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.InputDecorator}</li>
     * <li>outputDecorator: fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.OutputDecorator}</li>
     * </ul>
     *
     * @param obj         the JNDI name of {@code com.fasterxml.jackson.core.JsonFactory} resource
     * @param name        always null
     * @param nameCtx     always null
     * @param environment a {@code Hashtable} of configuration properties
     * @return an instance of {@code com.fasterxml.jackson.core.JsonFactory}
     * @throws Exception any exception occurred
     */
    @Override
    public Object getObjectInstance(final Object obj,
                                    final Name name,
                                    final Context nameCtx,
                                    final Hashtable<?, ?> environment) throws Exception {
        final JsonFactory jsonFactory = new JsonFactory();
        final Object jsonFactoryFeatures = environment.get("jsonFactoryFeatures");
        if (jsonFactoryFeatures != null) {
            configureJsonFactoryFeatures(jsonFactory, (String) jsonFactoryFeatures);
        }
        configureInputDecoratorAndOutputDecorator(jsonFactory, environment);
        return jsonFactory;
    }

    static void configureInputDecoratorAndOutputDecorator(final JsonFactory jsonFactory, final Hashtable<?, ?> environment)
            throws Exception {
        final Object inputDecorator = environment.get("inputDecorator");
        if (inputDecorator != null) {
            final Class<?> inputDecoratorClass = JsonFactoryObjectFactory.class.getClassLoader().loadClass((String) inputDecorator);
            jsonFactory.setInputDecorator((InputDecorator) inputDecoratorClass.newInstance());
        }

        final Object outputDecorator = environment.get("outputDecorator");
        if (outputDecorator != null) {
            final Class<?> outputDecoratorClass = JsonFactoryObjectFactory.class.getClassLoader().loadClass((String) outputDecorator);
            jsonFactory.setOutputDecorator((OutputDecorator) outputDecoratorClass.newInstance());
        }
    }

    static void configureJsonFactoryFeatures(final JsonFactory jsonFactory, final String jsonFactoryFeatures) {
        final StringTokenizer st = new StringTokenizer(jsonFactoryFeatures, ",");
        while (st.hasMoreTokens()) {
            final String[] pair = parseSingleFeatureValue(st.nextToken().trim());
            final String key = pair[0];
            final String value = pair[1];
            final JsonFactory.Feature feature;
            try {
                feature = JsonFactory.Feature.valueOf(key);
            } catch (final Exception e1) {
                throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
            }
            if ("true".equals(value)) {
                if (!feature.enabledByDefault()) {
                    jsonFactory.configure(feature, true);
                }
            } else if ("false".equals(value)) {
                if (feature.enabledByDefault()) {
                    jsonFactory.configure(feature, false);
                }
            } else {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
            }
        }
    }

    static String[] parseSingleFeatureValue(final String pair) {
        final int i = pair.indexOf('=');
        final String[] result = new String[2];
        if (i > 0) {
            result[0] = pair.substring(0, i).trim();
            result[1] = pair.substring(i + 1).trim();
        } else {
            result[0] = pair;
            result[1] = null;
        }
        return result;
    }
}
