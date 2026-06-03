/*
 * Copyright (c) 2020 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.spi;

import org.jberet.util.BatchUtil;

import java.io.IOException;
import java.io.Serializable;

public interface SerializableDataProvider {
    byte[] objectToBytes(final Object obj) throws IOException;

    Serializable bytesToObject(final byte[] bytes, final Class<?> klass, final ClassLoader classLoader) throws IOException, ClassNotFoundException;

    class DefaultSerializableDataProvider implements SerializableDataProvider {
        @Override
        public byte[] objectToBytes(final Object obj) throws IOException {
            return BatchUtil.objectToBytes(obj);
        }

        @Override
        public Serializable bytesToObject(final byte[] bytes, final Class<?> klass, final ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
            return BatchUtil.bytesToSerializableObject(bytes, classLoader);
        }
    }
}
