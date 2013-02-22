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
import javax.batch.runtime.context.BatchContext;

public abstract class BatchContextImpl<T> implements BatchContext<T> {
    protected String id;
    protected T transientUserData;
    protected String batchStatus;
    protected String exitStatus = batchStatus;
    protected ClassLoader classLoader;

    //not initialized here
    protected List<BatchContext<T>> batchContexts;

    protected JobContextImpl<T> jobContext;

    protected BatchContextImpl(String id) {
        this.id = id;
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

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    @Override
    public String getExitStatus() {
        if (exitStatus == null) {
            exitStatus = batchStatus;
        }
        return exitStatus;
    }

    @Override
    public void setExitStatus(String status) {
        if (status != null) {
            this.exitStatus = status;
        }
    }

    public JobContextImpl<T> getJobContext() {
        return this.jobContext;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
