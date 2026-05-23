/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobFactory;
import org.jberet.spi.SerializableDataProvider;

class SecurityActions {

    static Job cloneJob(final Job job) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Job>) () -> JobFactory.cloneJob(job));
        } else {
            return JobFactory.cloneJob(job);
        }
    }

    static SerializableDataProvider loadSerializableDataProvider() {
        final PrivilegedAction<SerializableDataProvider> action = () -> {
            final ServiceLoader<SerializableDataProvider> serviceLoader =
                    ServiceLoader.load(SerializableDataProvider.class);
            if (serviceLoader.iterator().hasNext()) {
                return serviceLoader.iterator().next();
            }
            return new SerializableDataProvider.DefaultSerializableDataProvider();
        };
        return System.getSecurityManager() != null
                ? AccessController.doPrivileged(action) : action.run();
    }
}
