/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.spi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jberet._private.BatchMessages;
import org.jberet.util.Assertions;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Uses the {@linkplain Thread#getContextClassLoader() context class loader} to find the {@link JobOperatorContext} for.
 * If a context os not found for the loader the {@linkplain DefaultJobOperatorContextSelector default context} will be
 * used.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("unused")
public class ContextClassLoaderJobOperatorContextSelector implements JobOperatorContextSelector {
    private final JobOperatorContextSelector defaultSelector;
    private final ConcurrentMap<ClassLoader, JobOperatorContext> contexts = new ConcurrentHashMap<>();
    private final PrivilegedAction<JobOperatorContext> action = new PrivilegedAction<JobOperatorContext>() {
        @Override
        public JobOperatorContext run() {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                final JobOperatorContext context = contexts.get(cl);
                if (context != null) {
                    return context;
                }
            }
            return defaultSelector.getJobOperatorContext();
        }
    };

    /**
     * Creates a new selector.
     *
     * @param defaultSelector the default selector, cannot be null
     */
    public ContextClassLoaderJobOperatorContextSelector(final JobOperatorContextSelector defaultSelector) {
        this.defaultSelector = Assertions.notNull(defaultSelector, "defaultSelector");
    }

    @Override
    public JobOperatorContext getJobOperatorContext() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(action);
        }
        return action.run();
    }

    /**
     * Registers a context with the class loader.
     *
     * @param classLoader the class loader to register the context with
     * @param context     the context that should be used on the class loader
     */
    public void registerContext(final ClassLoader classLoader, final JobOperatorContext context) {
        if (contexts.putIfAbsent(classLoader, context) != null) {
            // Don't allow a duplicate to be registered
            throw BatchMessages.MESSAGES.classLoaderAlreadyRegistered(classLoader);
        }
    }

    /**
     * Attempts to remove the context registered for the class loader. If the class loader does not have a registered
     * context {@code null} will be returned.
     *
     * @param classLoader the class loader to remove the context for
     *
     * @return the removed context or {@code null} if the class loader did not have a registered context
     */
    public JobOperatorContext unregisterContext(final ClassLoader classLoader) {
        return contexts.remove(classLoader);
    }

    /**
     * Attempts to remove the specific context registered for the class loader. If the context is not registered with
     * the specified context {@code false} will be returned.
     *
     * @param classLoader the class loader to remove the context for
     * @param context     the context which should be removed
     *
     * @return {@code true} if the context was removed, otherwise {@code false}
     */
    public boolean unregisterContext(final ClassLoader classLoader, final JobOperatorContext context) {
        return contexts.remove(classLoader, context);
    }
}
