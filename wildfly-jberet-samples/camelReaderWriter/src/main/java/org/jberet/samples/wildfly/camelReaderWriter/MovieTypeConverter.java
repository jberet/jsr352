/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.camelReaderWriter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.support.TypeConverterSupport;

public class MovieTypeConverter extends TypeConverterSupport {
    @SuppressWarnings("unchecked")
    public <T> T convertTo(final Class<T> type, final Exchange exchange, final Object value) throws TypeConversionException {
        final InputStream is = new ByteArrayInputStream(value.toString().getBytes());
        return (T) is;
    }
}
