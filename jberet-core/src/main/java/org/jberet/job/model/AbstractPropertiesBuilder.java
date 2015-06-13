/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.job.model;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
abstract class AbstractPropertiesBuilder<T> {
    final Map<String, String> nameValues = new LinkedHashMap<String, String>();

    public T property(final String k, final String v) {
        nameValues.put(k, v);
        return (T) this;
    }

    public T properties(final java.util.Properties props) {
        if (props != null) {
            for (final String k : props.stringPropertyNames()) {
                nameValues.put(k, props.getProperty(k));
            }
        }
        return (T) this;
    }

    public T properties(final String[]... propKeysValues) {
        for (final String[] pair : propKeysValues) {
            nameValues.put(pair[0], pair.length > 1 ? pair[1] : null);
        }
        return (T) this;
    }

    Properties nameValuesToProperties(final Map<String, String> nameValues) {
        final Properties properties = new Properties();
        for (final Map.Entry<String, String> e : nameValues.entrySet()) {
            properties.add(e.getKey(), e.getValue());
        }
        return properties;
    }
}
