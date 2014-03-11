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
    private static final long serialVersionUID = 1426640914765713066L;
    private final String flowId;

    /**
     * indicates the current flow and the entire job execution is ended via transition element, as opposed to natural
     * completion.  In both cases, the batch status is COMPLETED. If the flow is part of a split, the flow can only
     * access a cloned {@link org.jberet.runtime.context.JobContextImpl}, so we need a way to record the fact that
     * this flow and the job are terminated via transition elements.
     */
    private boolean ended;

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

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(final boolean ended) {
        this.ended = ended;
    }
}
