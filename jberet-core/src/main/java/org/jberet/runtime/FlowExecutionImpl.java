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

package org.jberet.runtime;

import javax.batch.runtime.StepExecution;

public final class FlowExecutionImpl extends AbstractExecution {
    private final String flowId;

    /**
     * The last StepExecution of the current flow.  Needed if the next element after the current flow is a decison, or
     * if this flow is part of a split, and the next element after the split is a decision.
     */
    private StepExecution lastStepExecution;

    public FlowExecutionImpl(final String flowId) {
        this.flowId = flowId;
    }

    public String getFlowId() {
        return flowId;
    }

    public StepExecution getLastStepExecution() {
        return lastStepExecution;
    }

    public void setLastStepExecution(final StepExecution lastStepExecution) {
        this.lastStepExecution = lastStepExecution;
    }

}
