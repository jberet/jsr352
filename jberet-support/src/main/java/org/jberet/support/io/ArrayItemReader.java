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
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from an array
 * of data. The input {@link #resource} may be specified as a url or path to external resource,
 * or an inlined content. In either case, the array resource content should be enclosed
 * inside {@code []}. For example,
 * <pre>
 * &lt;property name="resource" value='["a", "b", "c"]'/&gt;
 *
 * &lt;property name="resource" value="[1, 2, 3]"/&gt;
 * &lt;property name="beanType" value="java.lang.Integer"/&gt;
 *
 * &lt;property name="resource" value="movies-2012.json"/&gt;
 * &lt;property name="beanType" value="org.jberet.support.io.Movie"/&gt;
 *
 * &lt;property name="beanType" value="org.jberet.support.io.Movie"/&gt;
 * &lt;property name="resource" value='[
 * {"rank" : 1, "tit" : "Number One", "grs" : 1000, "opn" : "2017-01-01"},
 * {"rank" : 2, "tit" : "Number Two", "grs" : 2000, "opn" : "2017-02-02"},
 * {"rank" : 3, "tit" : "Number Three", "grs" : 3000, "opn" : "2017-03-03"},
 * {"rank" : 4, "tit" : "Number Four", "grs" : 4000, "opn" : "2017-04-04"},
 * {"rank" : 5, "tit" : "Number Five", "grs" : 5000, "opn" : "2017-05-05"}
 * ]'/&gt;
 * </pre>
 * The default data type is {@code String}, and {@link #beanType} batch property
 * can be used to specify the intended data type, which can be {@code String},
 * any primitive wrapper type, or custom POJO.
 *
 * @since 1.3.0.Beta6
 */
@Named
@Dependent
public class ArrayItemReader extends JsonItemReader {
    private Object[] values;

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
