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
 
package org.jberet.repository;

import java.lang.ref.ReferenceQueue;

/**
 * A specialized {@code SoftReference} with a key that identifies this reference in a {@code Map}.
 *
 * @param <T> the type of the referent
 * @param <K> the key to identify this reference within a {@code Map}
 *
 * @since 1.1.0.Beta2
 */
final class SoftReference<T, K> extends java.lang.ref.SoftReference<T> {
    private final K key;

    public SoftReference(final T referent, final ReferenceQueue<? super T> q, final K key) {
        super(referent, q);
        this.key = key;
    }

    public K getKey() {
        return key;
    }
}
