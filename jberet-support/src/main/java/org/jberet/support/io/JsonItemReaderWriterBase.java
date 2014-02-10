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

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public abstract class JsonItemReaderWriterBase extends ItemReaderWriterBase {
    @Inject
    @BatchProperty
    protected String CANONICALIZE_FIELD_NAMES;

    @Inject
    @BatchProperty
    protected String INTERN_FIELD_NAMES;

    protected JsonFactory jsonFactory;

    protected void open(final Serializable checkpoint) throws Exception {
        jsonFactory = new MappingJsonFactory();
        if ("false".equals(CANONICALIZE_FIELD_NAMES)) {
            jsonFactory.configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, false);
        }
        if ("false".equals(INTERN_FIELD_NAMES)) {
            jsonFactory.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false);
        }
    }
}
