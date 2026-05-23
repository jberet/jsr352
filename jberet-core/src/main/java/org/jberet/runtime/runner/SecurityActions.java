/*
 * Copyright (c) 2014-2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.runner;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.spi.PartitionHandlerFactory;

class SecurityActions {

    static PartitionHandlerFactory loadPartitionHandlerFactory() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(LOADER_ACTION);
        } else {
            return LOADER_ACTION.run();
        }
    }

    private static final PrivilegedAction<PartitionHandlerFactory> LOADER_ACTION = () -> {
        final ServiceLoader<PartitionHandlerFactory> serviceLoader = ServiceLoader.load(PartitionHandlerFactory.class);
        if (serviceLoader.iterator().hasNext()) {
            return serviceLoader.iterator().next();
        }
        return null;
    };
}
