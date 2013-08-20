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

import org.jberet.job.model.Flow;
import org.jberet.runtime.FlowExecutionImpl;

public class FlowContextImpl extends AbstractContext {
    private final Flow flow;

    private final FlowExecutionImpl flowExecution;

    public FlowContextImpl(final Flow flow, final AbstractContext[] outerContexts) {
        super(flow.getId(), outerContexts);
        this.flow = flow;
        this.classLoader = getJobContext().getClassLoader();
        this.flowExecution = new FlowExecutionImpl(id);
        this.flowExecution.setBatchStatus(BatchStatus.STARTING);
    }

    public Flow getFlow() {
        return this.flow;
    }

    public FlowExecutionImpl getFlowExecution() {
        return flowExecution;
    }

    public BatchStatus getBatchStatus() {
        return flowExecution.getBatchStatus();
    }

    @Override
    public void setBatchStatus(final BatchStatus status) {
        flowExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return flowExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(final String exitStatus) {
        flowExecution.setExitStatus(exitStatus);
    }
}
