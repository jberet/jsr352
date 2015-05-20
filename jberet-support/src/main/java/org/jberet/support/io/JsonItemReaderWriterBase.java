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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for {@link org.jberet.support.io.JsonItemReader} and {@link org.jberet.support.io.JsonItemWriter}.
 * It also holds Json-related common batch properties for customizing Json processing.
 *
 * @see     JsonItemReader
 * @see     JsonItemWriter
 * @since   1.0.2
 */
public abstract class JsonItemReaderWriterBase extends ItemReaderWriterBase {
    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.core.JsonFactory.Feature}s.
     * Optional property and defaults to null. Only keys and values defined in
     * {@code com.fasterxml.jackson.core.JsonFactory.Feature} are accepted. For example,
     * <p>
     * <pre>
     * INTERN_FIELD_NAMES=false, CANONICALIZE_FIELD_NAMES=false
     * </pre>
     *
     * @see "com.fasterxml.jackson.core.JsonFactory.Feature"
     * @see NoMappingJsonFactoryObjectFactory#configureJsonFactoryFeatures(com.fasterxml.jackson.core.JsonFactory, java.lang.String)
     */
    @Inject
    @BatchProperty
    protected String jsonFactoryFeatures;

    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.databind.ObjectMapper}
     * features. Optional property and defaults to null. Only keys and values defined in
     * {@code com.fasterxml.jackson.databind.MapperFeature} are accepted. For example,
     * <p>
     * <pre>
     * USE_ANNOTATIONS=false, AUTO_DETECT_FIELDS=false, AUTO_DETECT_GETTERS=true
     * </pre>
     *
     * @see "com.fasterxml.jackson.databind.MapperFeature"
     * @see MappingJsonFactoryObjectFactory#configureMapperFeatures(com.fasterxml.jackson.databind.ObjectMapper, java.lang.String)
     */
    @Inject
    @BatchProperty
    protected String mapperFeatures;

    /**
     * JNDI lookup name for {@code com.fasterxml.jackson.core.JsonFactory}, which is used for constructing
     * {@code com.fasterxml.jackson.core.JsonParser} in {@link org.jberet.support.io.JsonItemReader} and
     * {@code com.fasterxml.jackson.core.JsonGenerator} in {@link org.jberet.support.io.JsonItemWriter}.
     * Optional property and defaults to null. When this property is specified, its value is used to look up an
     * instance of {@code com.fasterxml.jackson.core.JsonFactory}, which is typically created and
     * administrated externally (e.g., inside application server). Otherwise, a new instance of
     * {@code com.fasterxml.jackson.core.JsonFactory} is created instead of lookup.
     *
     * @see "com.fasterxml.jackson.core.JsonFactory"
     */
    @Inject
    @BatchProperty
    protected String jsonFactoryLookup;

    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.databind.SerializationFeature}s.
     *  Optional property and defaults to null. Only keys and values defined in
     *  {@code com.fasterxml.jackson.databind.SerializationFeature} are accepted. For example,
     *  <p>
     *  <pre>
     *  WRAP_ROOT_VALUE=true, INDENT_OUTPUT=true, FAIL_ON_EMPTY_BEANS=false
     *  </pre>
     *
     * @see "com.fasterxml.jackson.databind.SerializationFeature"
     * @see MappingJsonFactoryObjectFactory#configureSerializationFeatures(com.fasterxml.jackson.databind.ObjectMapper, java.lang.String)
     */
    @Inject
    @BatchProperty
    protected String serializationFeatures;

    /**
     * A comma-separated list of fully-qualified name of classes that implement
     * {@code com.fasterxml.jackson.databind.JsonSerializer}, which can serialize Objects of
     * arbitrary types into JSON. For example,
     * <p>
     * <pre>
     * org.jberet.support.io.JsonItemReaderTest$JsonSerializer, org.jberet.support.io.JsonItemReaderTest$JsonSerializer2
     * </pre>
     *
     * @see "com.fasterxml.jackson.databind.JsonSerializer"
     * @see MappingJsonFactoryObjectFactory#configureCustomSerializersAndDeserializers
     */
    @Inject
    @BatchProperty
    protected String customSerializers;

    /**
     * A comma-separated list of key-value pairs that specify {@code com.fasterxml.jackson.databind.DeserializationFeature}s.
     * Optional property and defaults to null. Only keys and values defined in
     * {@code com.fasterxml.jackson.databind.DeserializationFeature} are accepted. For example,
     * <p>
     * <pre>
     * USE_BIG_DECIMAL_FOR_FLOATS=true, USE_BIG_INTEGER_FOR_INTS=true, USE_JAVA_ARRAY_FOR_JSON_ARRAY=true
     * </pre>
     * @see "com.fasterxml.jackson.databind.DeserializationFeature"
     * @see MappingJsonFactoryObjectFactory#configureDeserializationFeatures(com.fasterxml.jackson.databind.ObjectMapper, java.lang.String)
     */
    @Inject
    @BatchProperty
    protected String deserializationFeatures;

    /**
     * A comma-separated list of fully-qualified name of classes that implement
     * {@code com.fasterxml.jackson.databind.JsonDeserializer}, which can deserialize Objects of
     * arbitrary types from JSON. For example,
     * <p>
     * <pre>
     * org.jberet.support.io.JsonItemReaderTest$JsonDeserializer, org.jberet.support.io.JsonItemReaderTest$JsonDeserializer2
     * </pre>
     *
     * @see "com.fasterxml.jackson.databind.JsonDeserializer"
     * @see MappingJsonFactoryObjectFactory#configureCustomSerializersAndDeserializers
     */
    @Inject
    @BatchProperty
    protected String customDeserializers;

    /**
     * A comma-separated list of Jackson datatype module type ids that extend {@code com.fasterxml.jackson.databind.Module}.
     * These modules will be registered with {@link #objectMapper}. For example,
     * <p>
     * <pre>
     * com.fasterxml.jackson.datatype.joda.JodaModule, com.fasterxml.jackson.datatype.jsr353.JSR353Module, com.fasterxml.jackson.datatype.jsr310.JSR310Module
     * </pre>
     *
     * @see MappingJsonFactoryObjectFactory#configureCustomSerializersAndDeserializers
     */
    @Inject
    @BatchProperty
    protected String customDataTypeModules;

    protected JsonFactory jsonFactory;
    protected ObjectMapper objectMapper;

    /**
     * Registers any {@code com.fasterxml.jackson.databind.module.SimpleModule} to the {@link #objectMapper}. Any number
     * of custom serializers or deserializers can be added to the module.
     */
    protected void registerModule() throws Exception {
        MappingJsonFactoryObjectFactory.configureCustomSerializersAndDeserializers(
                objectMapper, customSerializers, customDeserializers, customDataTypeModules, getClass().getClassLoader());
    }

    /**
     * Initializes {@code JsonFactory} or its subtypes, e.g., {@code com.fasterxml.jackson.dataformat.xml.XmlFactory},
     * or {@code com.fasterxml.jackson.dataformat.csv.CsvFactory}. The factory should be initialized with a valid
     * {@code ObjectMapper}.
     *
     * Subclass may override this method to use different concrete types of {@code JsonFactory}.
     *
     * @throws Exception
     * @since 1.2.0.Alpha1
     */
    protected void initJsonFactory() throws Exception {
        if (jsonFactoryLookup != null) {
            jsonFactory = InitialContext.doLookup(jsonFactoryLookup);
        } else {
            jsonFactory = new MappingJsonFactory();
        }
    }

    /**
     * Initializes {@link #jsonFactory} and {@link #objectMapper} fields, which may be instantiated or obtained from
     * other part of the application. This method also configures the {@link #jsonFactory}, {@link #objectMapper},
     * {@link #serializationFeatures}, {@link #deserializationFeatures}, {@link #customDeserializers}, and
     * {@link #customSerializers} properly based on the current batch artifact properties.
     */
    protected void initJsonFactoryAndObjectMapper() throws Exception {
        initJsonFactory();
        objectMapper = (ObjectMapper) jsonFactory.getCodec();
        if (jsonFactoryFeatures != null) {
            NoMappingJsonFactoryObjectFactory.configureJsonFactoryFeatures(jsonFactory, jsonFactoryFeatures);
        }
        if (mapperFeatures != null) {
            MappingJsonFactoryObjectFactory.configureMapperFeatures(objectMapper, mapperFeatures);
        }
        if (deserializationFeatures != null) {
            MappingJsonFactoryObjectFactory.configureDeserializationFeatures(objectMapper, deserializationFeatures);
        }
        if (serializationFeatures != null) {
            MappingJsonFactoryObjectFactory.configureSerializationFeatures(objectMapper, serializationFeatures);
        }
        registerModule();
    }
}
