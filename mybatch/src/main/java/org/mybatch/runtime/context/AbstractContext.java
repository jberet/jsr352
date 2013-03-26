/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.runtime.context;

import javax.batch.runtime.BatchStatus;

import org.mybatch.job.Properties;

public abstract class AbstractContext {
    protected String id;
    protected Object transientUserData;
    protected ClassLoader classLoader;

    /**
     * Chain of batch contexts, the first one is the root JobContext.
     * If this is a JobContext type, outerContexts is null (no outer context);
     * if this is a StepContext type directly under a job, outerContexts contains 1 element (the root JobContext)
     */
    protected AbstractContext[] outerContexts;

    public abstract void setBatchStatus(BatchStatus status);

    public abstract BatchStatus getBatchStatus();

    public abstract void setExitStatus(String status);

    public abstract String getExitStatus();

    /**
     * Gets the org.mybatch.job.Properties configured for the element corresponding to this BatchContext.
     *
     * @return org.mybatch.job.Properties
     */
    public abstract Properties getProperties2();

    protected AbstractContext(String id) {
        this.id = id;
    }

    protected AbstractContext(String id, AbstractContext[] outerContexts) {
        this.id = id;
        this.outerContexts = outerContexts;
    }

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

