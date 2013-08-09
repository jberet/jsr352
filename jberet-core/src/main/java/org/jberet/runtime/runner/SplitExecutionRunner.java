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

package org.jberet.runtime.runner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.job.model.Flow;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Split;
import org.jberet.runtime.FlowExecutionImpl;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.SplitContextImpl;

import static org.jberet.util.BatchLogger.LOGGER;

public final class SplitExecutionRunner extends CompositeExecutionRunner<SplitContextImpl> implements Runnable {
    private static final long SPLIT_FLOW_TIMEOUT_SECONDS = 300;
    private Split split;

    public SplitExecutionRunner(SplitContextImpl splitContext, CompositeExecutionRunner enclosingRunner) {
        super(splitContext, enclosingRunner);
        this.split = splitContext.getSplit();
    }

    @Override
    protected List<? extends JobElement> getJobElements() {
        return split.getFlows();
    }

    @Override
    public void run() {
        batchContext.setBatchStatus(BatchStatus.STARTED);
        List<Flow> flows = split.getFlows();
        CountDownLatch latch = new CountDownLatch(flows.size());
        try {
            for (Flow f : flows) {
                runFlow(f, latch);
            }
            latch.await(SPLIT_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            //check FlowResults from each flow
            List<FlowExecutionImpl> fes = batchContext.getFlowExecutions();
            for (int i = 0; i < fes.size(); i++) {
                if (fes.get(i).getBatchStatus().equals(BatchStatus.FAILED)) {
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    for (AbstractContext c : batchContext.getOuterContexts()) {
                        c.setBatchStatus(BatchStatus.FAILED);
                    }
                    break;
                }
            }
            if (batchContext.getBatchStatus().equals(BatchStatus.STARTED)) {
                batchContext.setBatchStatus(BatchStatus.COMPLETED);
            }
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, jobContext.getJobName(), split.getId(), split);
            for (AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(BatchStatus.FAILED);
            }
        }

        if (batchContext.getBatchStatus() == BatchStatus.COMPLETED) {
            String next = split.getAttributeNext();  //split has no transition elements
            if (next != null) {
                //the last StepExecution of each flow is needed if the next element after this split is a decision
                List<FlowExecutionImpl> fes = batchContext.getFlowExecutions();
                StepExecution[] stepExecutions = new StepExecution[fes.size()];
                for (int i = 0; i < fes.size(); i++) {
                    stepExecutions[i] = fes.get(i).getLastStepExecution();
                }
                enclosingRunner.runJobElement(next, stepExecutions);
            }
        }
    }

}
