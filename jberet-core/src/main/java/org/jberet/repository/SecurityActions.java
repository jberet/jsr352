/*
 * Copyright (c) 2014-2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;
  
import java.security.AccessController;
import java.security.PrivilegedAction;

class SecurityActions {

    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        }
        return Thread.currentThread().getContextClassLoader();
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) clazz::getClassLoader);
        }
        return clazz.getClassLoader();
    }
}
