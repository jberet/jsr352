/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.LinkedList;
import javax.batch.api.listener.StepListener;
import javax.batch.operations.JobOperator;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Step;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchLogger;
import org.mybatch.util.BatchUtil;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class StepExecutionRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    private Step step;
    private Object stepResult;

    public StepExecutionRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner) {
        super(stepContext, enclosingRunner);
        this.step = stepContext.getStep();
    }

    @Override
    public void run() {
        LinkedList<Step> executedSteps = batchContext.getJobContext().getExecutedSteps();
        if (executedSteps.contains(step)) {
            StringBuilder stepIds = BatchUtil.toElementSequence(executedSteps);
            stepIds.append(step.getId());
            throw LOGGER.loopbackStep(step.getId(), stepIds.toString());
        }

        batchContext.setBatchStatus(JobOperator.BatchStatus.STARTED);
        batchContext.getJobContext().setBatchStatus(JobOperator.BatchStatus.STARTED);

        try {
            Chunk chunk = step.getChunk();
            Batchlet batchlet = step.getBatchlet();
            if (chunk == null && batchlet == null) {
                batchContext.setBatchStatus(JobOperator.BatchStatus.ABANDONED);
                LOGGER.stepContainsNoChunkOrBatchlet(id);
                return;
            }

            if (chunk != null && batchlet != null) {
                batchContext.setBatchStatus(JobOperator.BatchStatus.ABANDONED);
                LOGGER.cannotContainBothChunkAndBatchlet(id);
                return;
            }

            for (StepListener l : batchContext.getStepListeners()) {
                try {
                    l.beforeStep();
                } catch (Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, l, "beforeStep");
                    batchContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                    return;
                }
            }


            BatchletRunner batchletRunner = new BatchletRunner(batchContext, enclosingRunner, batchlet);
            stepResult = batchletRunner.call();

            //TODO handle chunk type step


            //record the fact this step has been executed
            executedSteps.add(step);

            for (StepListener l : batchContext.getStepListeners()) {
                try {
                    l.afterStep();
                } catch (Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, l, "afterStep");
                    batchContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                    return;
                }
            }
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, step, "run");
            if (e instanceof Exception) {
                batchContext.setException((Exception) e);
            }
            batchContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
            for (AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(JobOperator.BatchStatus.FAILED);
            }
        }

        if (batchContext.getBatchStatus() == JobOperator.BatchStatus.STARTED) {  //has not been marked as failed, stopped or abandoned
            batchContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED);
        }

        if (batchContext.getBatchStatus() == JobOperator.BatchStatus.COMPLETED) {
            String next = step.getNext();
            if (next == null) {
                next = resolveControlElements(step.getTransitionElements());
            }
            enclosingRunner.runJobElement(next, batchContext.getStepExecution());
        }
    }

}
