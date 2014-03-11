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
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.job.model.Flow;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Split;
import org.jberet.runtime.FlowExecutionImpl;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.SplitContextImpl;

import static org.jberet._private.BatchLogger.LOGGER;

public final class SplitExecutionRunner extends CompositeExecutionRunner<SplitContextImpl> implements Runnable {
    private final Split split;

    public SplitExecutionRunner(final SplitContextImpl splitContext, final CompositeExecutionRunner enclosingRunner) {
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
        final List<Flow> flows = split.getFlows();
        final CountDownLatch latch = new CountDownLatch(flows.size());
        boolean terminateSplit = false;
        try {
            for (final Flow f : flows) {
                runFlow(f, latch);
            }
            latch.await();

            //check FlowResults from each flow
            final List<FlowExecutionImpl> fes = batchContext.getFlowExecutions();
            FlowExecutionImpl failedFlow = null;
            FlowExecutionImpl stoppedFlow = null;
            FlowExecutionImpl endedFlow = null;
            for (int i = 0; i < fes.size(); i++) {
                final FlowExecutionImpl flowExecution = fes.get(i);
                if (flowExecution.getBatchStatus() == BatchStatus.FAILED) {
                    failedFlow = flowExecution;
                    break;
                } else if (flowExecution.getBatchStatus() == BatchStatus.STOPPED) {
                    stoppedFlow = flowExecution;
                } else if (flowExecution.getBatchStatus() == BatchStatus.COMPLETED) {
                    if (flowExecution.isEnded()) {
                        endedFlow = flowExecution;
                    }
                }
            }

            if (failedFlow != null || stoppedFlow != null || endedFlow != null) {
                terminateSplit = true;
                final BatchStatus splitBatchStatus;
                final String splitExitStatus;
                if (failedFlow != null) {
                    splitBatchStatus = failedFlow.getBatchStatus();
                    splitExitStatus = failedFlow.getExitStatus();
                } else if (stoppedFlow != null) {
                    splitBatchStatus = stoppedFlow.getBatchStatus();
                    splitExitStatus = stoppedFlow.getExitStatus();
                } else {
                    splitBatchStatus = endedFlow.getBatchStatus();
                    splitExitStatus = endedFlow.getExitStatus();
                }

                batchContext.setBatchStatus(splitBatchStatus);
                if (splitExitStatus != null) {
                    batchContext.setExitStatus(splitExitStatus);
                }
                for (final AbstractContext c : batchContext.getOuterContexts()) {
                    c.setBatchStatus(splitBatchStatus);
                    if (splitExitStatus != null) {
                        c.setExitStatus(splitExitStatus);
                    }
                }
            } else if (batchContext.getBatchStatus().equals(BatchStatus.STARTED)) {
                batchContext.setBatchStatus(BatchStatus.COMPLETED);
            }
        } catch (final Throwable e) {
            terminateSplit = true;
            LOGGER.failToRunJob(e, jobContext.getJobName(), split.getId(), split);
            for (final AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(BatchStatus.FAILED);
            }
        }

        if (!terminateSplit && batchContext.getBatchStatus() == BatchStatus.COMPLETED) {
            final String next = split.getAttributeNext();  //split has no transition elements
            if (next != null) {
                //the last StepExecution of each flow is needed if the next element after this split is a decision
                final List<FlowExecutionImpl> fes = batchContext.getFlowExecutions();
                final StepExecution[] stepExecutions = new StepExecution[fes.size()];
                for (int i = 0; i < fes.size(); i++) {
                    stepExecutions[i] = fes.get(i).getLastStepExecution();
                }
                enclosingRunner.runJobElement(next, stepExecutions);
            }
        }
    }

}
