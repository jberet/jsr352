/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
 
package org.jberet.job.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for those builder classes that take properties, such as {@link JobBuilder}, {@link StepBuilder}, and
 * {@link DecisionBuilder}.
 *
 * @param <T> subclass type so methods can return the concrete subclass type
 *
 * @see JobBuilder
 * @see StepBuilder
 * @see DecisionBuilder
 * @since 1.2.0
 */
@SuppressWarnings("unchecked")
abstract class AbstractPropertiesBuilder<T> {
    final Map<String, String> nameValues = new LinkedHashMap<String, String>();

    /**
     * Adds one single property to the current builder.
     *
     * @param k property key
     * @param v property value
     * @return current builder instance
     */
    public T property(final String k, final String v) {
        nameValues.put(k, v);
        return (T) this;
    }

    /**
     * Adds all properties in java.util.Properties to the current builder.
     *
     * @param props java.util.Properties
     * @return current builder instance
     */
    public T properties(final java.util.Properties props) {
        if (props != null) {
            for (final String k : props.stringPropertyNames()) {
                nameValues.put(k, props.getProperty(k));
            }
        }
        return (T) this;
    }

    /**
     * Adds 1 or multiple properties to the current builder. The properties are represented as a series of 2-element
     * string array, whose 1st element is property key, and 2nd element is property value. For example,
     * <p/>
     * <pre>
     * properties(new String[]{"stepk1", "value1"}, new String[]{"stepk2", "value2"})
     * </pre>
     *
     * @param pairsOfKeyValue a series of 2-element string array, whose 1st element is property key, and 2nd element is property value
     * @return current builder instance
     */
    public T properties(final String[]... pairsOfKeyValue) {
        for (final String[] pair : pairsOfKeyValue) {
            nameValues.put(pair[0], pair.length > 1 ? pair[1] : null);
        }
        return (T) this;
    }

    /**
     * Converts {@code Map<String, String>} to {@code org.jberet.job.model.Properties}.
     *
     * @return an instance of org.jberet.job.model.Properties
     */
    Properties nameValuesToProperties() {
        final Properties properties = new Properties();
        for (final Map.Entry<String, String> e : nameValues.entrySet()) {
            properties.add(e.getKey(), e.getValue());
        }
        return properties;
    }
}
