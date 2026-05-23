/*
 * Copyright (c) 2014-2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.operations;

import java.util.ServiceLoader;

import org.jberet.spi.BatchEnvironment;

class SecurityActions {
    static BatchEnvironment loadBatchEnvironment() {
        final ServiceLoader<BatchEnvironment> serviceLoader = ServiceLoader.load(BatchEnvironment.class);
        if (serviceLoader.iterator().hasNext()) {
            return serviceLoader.iterator().next();
        }
        return null;
    }
}
