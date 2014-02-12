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

import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.jberet.support._private.SupportLogger;

public abstract class JsonItemReaderWriterBase extends ItemReaderWriterBase {

    @Inject
    @BatchProperty
    protected Map<String, String> jsonFactoryFeatures;

    protected JsonFactory jsonFactory;

    /**
     * Initializes {@code jsonFactory} field, which may be instantiated or obtained from other part of the application.
     * This method also configures the {@code jsonFactory} properly.
     */
    protected void initJsonFactory() {
        jsonFactory = new MappingJsonFactory();
        if (jsonFactoryFeatures != null) {
            for (final Map.Entry<String, String> e : jsonFactoryFeatures.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
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
                    throw SupportLogger.LOGGER.invalidReaderWriterProperty(value, key);
                }
            }
        }
    }
}
