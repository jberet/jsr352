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

import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.Split;
import org.jberet.runtime.FlowExecutionImpl;
import org.jberet.runtime.SplitExecutionImpl;

public class SplitContextImpl extends AbstractContext {
    private Split split;

    private SplitExecutionImpl splitExecution;

    private List<FlowExecutionImpl> flowExecutions = new ArrayList<FlowExecutionImpl>();

    public SplitContextImpl(Split split, AbstractContext[] outerContexts) {
        super(split.getId(), outerContexts);
        this.split = split;
        this.classLoader = getJobContext().getClassLoader();
        this.splitExecution = new SplitExecutionImpl(split.getId());
        splitExecution.setBatchStatus(BatchStatus.STARTING);
    }

    public Split getSplit() {
        return split;
    }

    public List<FlowExecutionImpl> getFlowExecutions() {
        return this.flowExecutions;
    }

    @Override
    public BatchStatus getBatchStatus() {
        return splitExecution.getBatchStatus();
    }

    @Override
    public void setBatchStatus(BatchStatus status) {
        splitExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return splitExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(String exitStatus) {
        splitExecution.setExitStatus(exitStatus);
    }

    @Override
    public org.jberet.job.Properties getProperties2() {
        return null;  //split has no <properties>
    }

}
