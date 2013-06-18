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

import org.jberet.job.Flow;
import org.jberet.runtime.FlowExecutionImpl;

public class FlowContextImpl extends AbstractContext {
    private Flow flow;

    private FlowExecutionImpl flowExecution;

    public FlowContextImpl(Flow flow, AbstractContext[] outerContexts) {
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
    public void setBatchStatus(BatchStatus status) {
        flowExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return flowExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(String exitStatus) {
        flowExecution.setExitStatus(exitStatus);
    }

    @Override
    public org.jberet.job.Properties getProperties2() {
        return null;  //flow has no <properties>
    }

}
