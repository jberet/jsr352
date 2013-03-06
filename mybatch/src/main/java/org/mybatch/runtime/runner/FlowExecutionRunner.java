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
import javax.batch.operations.JobOperator;

import org.mybatch.job.Flow;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.FlowContextImpl;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class FlowExecutionRunner extends CompositeExecutionRunner implements Runnable {
    private Flow flow;
    private CountDownLatch latch;
    private FlowContextImpl flowContext;

    public FlowExecutionRunner(FlowContextImpl flowContext, CompositeExecutionRunner enclosingRunner, CountDownLatch latch) {
        super(flowContext, enclosingRunner);
        this.flow = flowContext.getFlow();
        this.flowContext = flowContext;
        this.latch = latch;
    }

    @Override
    protected List<?> getJobElements() {
        return flow.getDecisionOrStepOrSplit();
    }

    @Override
    public void run() {
        batchContext.setBatchStatus(JobOperator.BatchStatus.STARTED);
        batchContext.getJobContext().setBatchStatus(JobOperator.BatchStatus.STARTED);

        try {
            runFromHead();
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, flow, "run");
            batchContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
            for (AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(JobOperator.BatchStatus.FAILED);
            }
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }

        if (batchContext.getBatchStatus() == JobOperator.BatchStatus.STARTED) {  //has not been marked as failed, stopped or abandoned
            batchContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED);
            enclosingRunner.getBatchContext().setBatchStatus(JobOperator.BatchStatus.COMPLETED);  //set job batch status COMPLETED, may be changed next
        }

        if (batchContext.getBatchStatus() == JobOperator.BatchStatus.COMPLETED) {
            String next = flow.getNext();
            if (next == null) {
                next = resolveControlElements(flow.getControlElements());
            }
            enclosingRunner.runJobElement(next, flowContext.getFlowExecution().getLastStepExecution());
        }
    }

}
