/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

final class ObjectInputStreamWithClassloader extends ObjectInputStream {
    private final ClassLoader loader;

    public ObjectInputStreamWithClassloader(final InputStream in, final ClassLoader loader) throws IOException {
        super(in);
        this.loader = loader;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            return Class.forName(desc.getName(), false, loader);
        } catch (final ClassNotFoundException ex) {
            final Class<?> cl = super.resolveClass(desc);
            if (cl != null) {
                return cl;
            }
            throw ex;
        }
    }
}
