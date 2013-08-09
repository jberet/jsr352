/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.context;

import javax.batch.runtime.BatchStatus;

public abstract class AbstractContext implements Cloneable {
    protected String id;
    protected Object transientUserData;
    protected ClassLoader classLoader;

    /**
     * Chain of batch contexts, the first one is the root JobContext.
     * If this is a JobContext type, outerContexts is null (no outer context);
     * if this is a StepContext type directly under a job, outerContexts contains 1 element (the root JobContext)
     */
    protected AbstractContext[] outerContexts;

    protected AbstractContext(String id) {
        this.id = id;
    }

    protected AbstractContext(String id, AbstractContext[] outerContexts) {
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

    public void setTransientUserData(Object data) {
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
    public static AbstractContext[] addToContextArray(AbstractContext[] contextArray, AbstractContext add) {
        if (contextArray == null) {
            return new AbstractContext[]{add};
        }
        AbstractContext[] result = new AbstractContext[contextArray.length + 1];
        System.arraycopy(contextArray, 0, result, 0, contextArray.length);
        result[contextArray.length] = add;
        return result;
    }
}

