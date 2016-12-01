/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.util;

import org.jberet._private.BatchMessages;

/**
 * Assertion utilities.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Assertions {

    /**
     * If the value is {@code null} an {@link IllegalArgumentException} is thrown, otherwise the value is returned.
     *
     * @param value the value to check
     * @param name  the name of the parameter being checked
     * @param <T>   the type
     *
     * @return the value if not {@code null}
     */
    public static <T> T notNull(final T value, final String name) {
        if (value == null) {
            throw BatchMessages.MESSAGES.nullVar(name);
        }
        return value;
    }
}
