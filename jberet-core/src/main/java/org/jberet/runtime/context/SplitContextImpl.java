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

import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.model.Split;
import org.jberet.runtime.FlowExecutionImpl;
import org.jberet.runtime.SplitExecutionImpl;

public class SplitContextImpl extends AbstractContext {
    private final Split split;

    private final SplitExecutionImpl splitExecution;

    private final List<FlowExecutionImpl> flowExecutions = new ArrayList<FlowExecutionImpl>();

    public SplitContextImpl(final Split split, final AbstractContext[] outerContexts) {
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
    public void setBatchStatus(final BatchStatus status) {
        splitExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return splitExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(final String exitStatus) {
        splitExecution.setExitStatus(exitStatus);
    }
}
