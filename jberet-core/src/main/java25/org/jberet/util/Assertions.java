/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
