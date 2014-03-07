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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.naming.spi.ObjectFactory} that produces instance of
 * {@code com.fasterxml.jackson.databind.MappingJsonFactory}. This class can be used to create a custom JNDI resource
 * in an application server. See wildfly.home/docs/schema/jboss-as-naming_2_0.xsd for more details.
 */
public final class MappingJsonFactoryObjectFactory implements ObjectFactory {
    private volatile MappingJsonFactory jsonFactoryCached;

    /**
     * Gets an instance of {@code com.fasterxml.jackson.databind.MappingJsonFactory} based on the resource configuration
     * in the application server. The parameter {@code environment} contains MappingJsonFactory configuration properties,
     * and accepts the following properties:
     * <ul>
     * <li>jsonFactoryFeatures: JsonFactory features as defined in com.fasterxml.jackson.core.JsonFactory.Feature</li>
     * <li>mapperFeatures: ObjectMapper features as defined in com.fasterxml.jackson.databind.MapperFeature</li>
     * <li>deserializationFeatures:</li>
     * <li>serializationFeatures:</li>
     * <li>customDeserializers:</li>
     * <li>customSerializers:</li>
     * <li>deserializationProblemHandlers:</li>
     * <li>inputDecorator: fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.InputDecorator}</li>
     * <li>outputDecorator: fully-qualified name of a class that extends {@code com.fasterxml.jackson.core.io.OutputDecorator}</li>
     * </ul>
     *
     * @param obj         the JNDI name of {@code com.fasterxml.jackson.databind.MappingJsonFactory} resource
     * @param name        always null
     * @param nameCtx     always null
     * @param environment a {@code Hashtable} of configuration properties
     * @return an instance of {@code com.fasterxml.jackson.databind.MappingJsonFactory}
     * @throws Exception any exception occurred
     */
    @Override
    public Object getObjectInstance(final Object obj,
                                    final Name name,
                                    final Context nameCtx,
                                    final Hashtable<?, ?> environment) throws Exception {
        MappingJsonFactory jsonFactory = jsonFactoryCached;
        if (jsonFactory == null) {
            synchronized (this) {
                jsonFactory = jsonFactoryCached;
                if (jsonFactory == null) {
                    jsonFactoryCached = jsonFactory = new MappingJsonFactory();
                }

                final ClassLoader classLoader = MappingJsonFactoryObjectFactory.class.getClassLoader();
                final ObjectMapper objectMapper = jsonFactory.getCodec();

                final Object jsonFactoryFeatures = environment.get("jsonFactoryFeatures");
                if (jsonFactoryFeatures != null) {
                    NonmappingJsonFactoryObjectFactory.configureJsonFactoryFeatures(jsonFactory, (String) jsonFactoryFeatures);
                }

                final Object mapperFeatures = environment.get("mapperFeatures");
                if (mapperFeatures != null) {
                    configureMapperFeatures(objectMapper, (String) mapperFeatures);
                }

                final Object deserializationFeatures = environment.get("deserializationFeatures");
                if (deserializationFeatures != null) {
                    configureDeserializationFeatures(objectMapper, (String) deserializationFeatures);
                }

                final Object serializationFeatures = environment.get("serializationFeatures");
                if (serializationFeatures != null) {
                    configureSerializationFeatures(objectMapper, (String) serializationFeatures);
                }

                final Object deserializationProblemHandlers = environment.get("deserializationProblemHandlers");
                if (deserializationProblemHandlers != null) {
                    configureDeserializationProblemHandlers(objectMapper, (String) deserializationProblemHandlers, classLoader);
                }

                configureCustomSerializersAndDeserializers(objectMapper, (String) environment.get("customSerializers"),
                        (String) environment.get("customDeserializers"), classLoader);
                NonmappingJsonFactoryObjectFactory.configureInputDecoratorAndOutputDecorator(jsonFactory, environment);
            }
        }
        return jsonFactory;
    }

    static void configureDeserializationProblemHandlers(final ObjectMapper objectMapper,
                                                        final String deserializationProblemHandlers,
                                                        final ClassLoader classLoader) throws Exception {
        final StringTokenizer st = new StringTokenizer(deserializationProblemHandlers, ", ");
        while (st.hasMoreTokens()) {
            final Class<?> c = classLoader.loadClass(st.nextToken());
            objectMapper.addHandler((DeserializationProblemHandler) c.newInstance());
        }
    }

    static void configureCustomSerializersAndDeserializers(final ObjectMapper objectMapper,
                                                           final String customSerializers,
                                                           final String customDeserializers,
                                                           final ClassLoader classLoader) throws Exception {
        if (customDeserializers == null && customSerializers == null) {
            return;
        }

        final SimpleModule simpleModule = new SimpleModule("custom-serializer-deserializer-module");
        if (customSerializers != null) {
            final StringTokenizer st = new StringTokenizer(customSerializers, ", ");
            while (st.hasMoreTokens()) {
                final Class<?> aClass = classLoader.loadClass(st.nextToken());
                simpleModule.addSerializer(aClass, (JsonSerializer) aClass.newInstance());
            }
        }
        if (customDeserializers != null) {
            final StringTokenizer st = new StringTokenizer(customDeserializers, ", ");
            while (st.hasMoreTokens()) {
                final Class<?> aClass = classLoader.loadClass(st.nextToken());
                simpleModule.addDeserializer(aClass, (JsonDeserializer) aClass.newInstance());
            }
        }
        objectMapper.registerModule(simpleModule);
    }

    static void configureMapperFeatures(final ObjectMapper objectMapper, final String features) {
        final StringTokenizer st = new StringTokenizer(features, ",");
        while (st.hasMoreTokens()) {
            final String[] pair = NonmappingJsonFactoryObjectFactory.parseSingleFeatureValue(st.nextToken().trim());
            final String key = pair[0];
            final String value = pair[1];

            final MapperFeature feature;
            try {
                feature = MapperFeature.valueOf(key);
            } catch (final Exception e1) {
                throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
            }
            if ("true".equals(value)) {
                if (!feature.enabledByDefault()) {
                    objectMapper.configure(feature, true);
                }
            } else if ("false".equals(value)) {
                if (feature.enabledByDefault()) {
                    objectMapper.configure(feature, false);
                }
            } else {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
            }
        }
    }

    static void configureSerializationFeatures(final ObjectMapper objectMapper, final String features) {
        final StringTokenizer st = new StringTokenizer(features, ",");
        while (st.hasMoreTokens()) {
            final String[] pair = NonmappingJsonFactoryObjectFactory.parseSingleFeatureValue(st.nextToken().trim());
            final String key = pair[0];
            final String value = pair[1];

            final SerializationFeature feature;
            try {
                feature = SerializationFeature.valueOf(key);
            } catch (final Exception e1) {
                throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
            }
            if ("true".equals(value)) {
                if (!feature.enabledByDefault()) {
                    objectMapper.configure(feature, true);
                }
            } else if ("false".equals(value)) {
                if (feature.enabledByDefault()) {
                    objectMapper.configure(feature, false);
                }
            } else {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
            }
        }
    }

    static void configureDeserializationFeatures(final ObjectMapper objectMapper, final String features) {
        final StringTokenizer st = new StringTokenizer(features, ",");
        while (st.hasMoreTokens()) {
            final String[] pair = NonmappingJsonFactoryObjectFactory.parseSingleFeatureValue(st.nextToken().trim());
            final String key = pair[0];
            final String value = pair[1];

            final DeserializationFeature feature;
            try {
                feature = DeserializationFeature.valueOf(key);
            } catch (final Exception e1) {
                throw SupportLogger.LOGGER.unrecognizedReaderWriterProperty(key, value);
            }
            if ("true".equals(value)) {
                if (!feature.enabledByDefault()) {
                    objectMapper.configure(feature, true);
                }
            } else if ("false".equals(value)) {
                if (feature.enabledByDefault()) {
                    objectMapper.configure(feature, false);
                }
            } else {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, value, key);
            }
        }
    }
}
