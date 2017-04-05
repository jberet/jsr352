/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
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

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from an array.
 *
 * @since 1.3.0.Beta6
 */
@Named
@Dependent
public class ArrayItemReader extends JsonItemReader {
    private Object[] values;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        if (beanType == null) {
            beanType = String.class;
        }
        final Object[] objs = (Object[]) Array.newInstance(beanType, 0);
        final Object inputSource = jsonParser.getInputSource();

        if (inputSource instanceof ByteArrayInputStream) {
            values = objectMapper.readValue(resource, objs.getClass());
        } else {
            values = objectMapper.readValue(jsonParser, objs.getClass());
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (values == null || rowNumber >= values.length || rowNumber > end) {
            return null;
        }
        Object obj = values[rowNumber++];
        SupportLogger.LOGGER.tracef("Read type %s, value %s%n", obj.getClass(), obj);
        return obj;
    }
}
