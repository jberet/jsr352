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

package org.jberet.runtime.runner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.Flow;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.FlowContextImpl;

import static org.jberet.util.BatchLogger.LOGGER;

public final class FlowExecutionRunner extends CompositeExecutionRunner<FlowContextImpl> implements Runnable {
    private Flow flow;
    private CountDownLatch latch;

    public FlowExecutionRunner(FlowContextImpl flowContext, CompositeExecutionRunner enclosingRunner, CountDownLatch latch) {
        super(flowContext, enclosingRunner);
        this.flow = flowContext.getFlow();
        this.latch = latch;
    }

    @Override
    protected List<?> getJobElements() {
        return flow.getDecisionOrFlowOrSplit();
    }

    @Override
    public void run() {
        batchContext.setBatchStatus(BatchStatus.STARTED);
        batchContext.getJobContext().setBatchStatus(BatchStatus.STARTED);

        try {
            runFromHeadOrRestartPoint(null);
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, batchContext.getJobContext().getJobName(), flow.getId(), flow);
            batchContext.setBatchStatus(BatchStatus.FAILED);
            for (AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(BatchStatus.FAILED);
            }
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }

        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {  //has not been marked as failed, stopped or abandoned
            batchContext.setBatchStatus(BatchStatus.COMPLETED);
        }

        if (batchContext.getBatchStatus() == BatchStatus.COMPLETED) {
            String next = resolveTransitionElements(flow.getTransitionElements(), flow.getNext(), false);
            enclosingRunner.runJobElement(next, batchContext.getFlowExecution().getLastStepExecution());
        }
    }

}
