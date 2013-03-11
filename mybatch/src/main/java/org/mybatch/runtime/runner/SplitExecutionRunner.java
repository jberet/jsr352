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

package org.mybatch.runtime.runner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.StepExecution;

import org.mybatch.job.Flow;
import org.mybatch.job.Split;
import org.mybatch.runtime.FlowExecutionImpl;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.SplitContextImpl;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class SplitExecutionRunner extends CompositeExecutionRunner<SplitContextImpl> implements Runnable {
    private static final long SPLIT_FLOW_TIMEOUT_SECONDS = 300;
    private Split split;

    public SplitExecutionRunner(SplitContextImpl splitContext, CompositeExecutionRunner enclosingRunner) {
        super(splitContext, enclosingRunner);
        this.split = splitContext.getSplit();
    }

    @Override
    protected List<?> getJobElements() {
        return split.getFlow();
    }

    @Override
    public void run() {
        batchContext.setBatchStatus(JobOperator.BatchStatus.STARTED);
        List<Flow> flows = split.getFlow();
        CountDownLatch latch = new CountDownLatch(flows.size());
        try {
            for (Flow f : flows) {
                runFlow(f, latch);
            }
            latch.await(SPLIT_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            //check FlowResults from each flow
            List<FlowExecutionImpl> fes = batchContext.getFlowExecutions();
            for (int i = 0; i < fes.size(); i++) {
                if (fes.get(i).getBatchStatus().equals(JobOperator.BatchStatus.FAILED)) {
                    batchContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                    for (AbstractContext c : batchContext.getOuterContexts()) {
                        c.setBatchStatus(JobOperator.BatchStatus.FAILED);
                    }
                    break;
                }
            }
            if (batchContext.getBatchStatus().equals(JobOperator.BatchStatus.STARTED)) {
                batchContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED);
                enclosingRunner.getBatchContext().setBatchStatus(JobOperator.BatchStatus.COMPLETED);
            }
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, split, "run");
            for (AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(JobOperator.BatchStatus.FAILED);
            }
        }

        if (batchContext.getBatchStatus() == JobOperator.BatchStatus.COMPLETED) {
            String next = split.getNext();
            if (next == null) {
                next = resolveControlElements(split.getControlElements());
            }

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
