/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.spi.Contextual;

import org.jberet.creation.JobScopedContextImpl;

public abstract class AbstractContext implements Cloneable {
    protected String id;
    protected Object transientUserData;
    protected ClassLoader classLoader;

    /**
     * A store for keeping CDI beans with {@link org.jberet.cdi.JobScoped} or {@link org.jberet.cdi.StepScoped}
     * custom scopes.
     * Cleared at the end of the execution of the respecitve job, step, or partition, and all
     * stored beans are destroyed by calling
     * {@link JobScopedContextImpl.ScopedInstance#destroy(ConcurrentMap)}
     */
    private final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> scopedBeans =
            new ConcurrentHashMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>>();

    /**
     * Chain of batch contexts, the first one is the root JobContext.
     * If this is a JobContext type, outerContexts is null (no outer context);
     * if this is a StepContext type directly under a job, outerContexts contains 1 element (the root JobContext)
     */
    protected AbstractContext[] outerContexts;

    protected AbstractContext(final String id) {
        this.id = id;
    }

    protected AbstractContext(final String id, final AbstractContext[] outerContexts) {
        this.id = id;
        this.outerContexts = outerContexts;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public abstract void setBatchStatus(BatchStatus status);

    public abstract BatchStatus getBatchStatus();

    public abstract void setExitStatus(String status);

    public abstract String getExitStatus();

    public String getId() {
        return id;
    }

    public Object getTransientUserData() {
        return transientUserData;
    }

    public void setTransientUserData(final Object data) {
        this.transientUserData = data;
    }

    public JobContextImpl getJobContext() {
        return (JobContextImpl) outerContexts[0];
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public AbstractContext[] getOuterContexts() {
        return outerContexts;
    }

    /**
     * Appends a BatchContext to an array of BatchContext and return the expanded new array.
     *
     * @param contextArray the array of BatchContext to add to; null if it's from a JobContext
     * @param add          the additional BatchContext; should not be nul
     * @return a newly expanded array of BatchContext
     */
    public static AbstractContext[] addToContextArray(final AbstractContext[] contextArray, final AbstractContext add) {
        if (contextArray == null) {
            return new AbstractContext[]{add};
        }
        final AbstractContext[] result = new AbstractContext[contextArray.length + 1];
        System.arraycopy(contextArray, 0, result, 0, contextArray.length);
        result[contextArray.length] = add;
        return result;
    }

    public ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> getScopedBeans() {
        return scopedBeans;
    }
}

