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

import java.util.List;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.context.BatchContext;

import org.mybatch.job.Properties;
import org.mybatch.util.PropertyResolver;

public abstract class AbstractContext<T> implements BatchContext<T> {
    protected String id;
    protected T transientUserData;
    protected ClassLoader classLoader;

    //not initialized here
    protected List<BatchContext<T>> batchContexts;

    /**
     * Chain of batch contexts, the first one is the root JobContext.
     * If this is a JobContext type, outerContexts is null (no outer context);
     * if this is a StepContext type directly under a job, outerContexts contains 1 element (the root JobContext)
     */
    protected AbstractContext<T>[] outerContexts;

    public abstract void setBatchStatus(JobOperator.BatchStatus status);

    /**
     * Gets the org.mybatch.job.Properties configured for the element corresponding to this BatchContext.
     *
     * @return org.mybatch.job.Properties
     */
    public abstract Properties getProperties2();

    protected AbstractContext(String id) {
        this.id = id;
    }

    protected AbstractContext(String id, AbstractContext<T>[] outerContexts) {
        this.id = id;
        this.outerContexts = outerContexts;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public T getTransientUserData() {
        return transientUserData;
    }

    @Override
    public void setTransientUserData(T data) {
        this.transientUserData = data;
    }

    @Override
    public List<BatchContext<T>> getBatchContexts() {
        return batchContexts;
    }

    public JobContextImpl<T> getJobContext() {
        return (JobContextImpl<T>) outerContexts[0];
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public AbstractContext<T>[] getOuterContexts() {
        return outerContexts;
    }

    /**
     * Appends a BatchContext to an array of BatchContext and return the expanded new array.
     *
     * @param contextArray the array of BatchContext to add to; null if it's from a JobContext
     * @param add          the additional BatchContext; should not be nul
     * @return a newly expanded array of BatchContext
     */
    public static <D> AbstractContext<D>[] addToContextArray(AbstractContext<D>[] contextArray, AbstractContext<D> add) {
        if (contextArray == null) {
            return new AbstractContext[]{add};
        }
        AbstractContext<D>[] result = new AbstractContext[contextArray.length + 1];
        System.arraycopy(contextArray, 0, result, 0, contextArray.length);
        result[contextArray.length] = add;
        return result;
    }

    protected PropertyResolver setUpPropertyResolver() {
        PropertyResolver resolver = new PropertyResolver();
        resolver.setJobParameters(getJobContext().getJobParameters());

        //this job element (job, step, flow, etc) can be directly under job, or under job->split->flow, etc
        org.mybatch.job.Properties props;
        if (outerContexts != null) {
            for (AbstractContext<T> context : outerContexts) {
                props = context.getProperties2();
                if (props != null) {
                    resolver.pushJobProperties(props);
                }
            }
        }

        props = getProperties2();
        if (props != null) {
            resolver.pushJobProperties(props);
        }
        return resolver;
    }

}

