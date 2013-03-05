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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.batch.api.StepListener;
import javax.batch.operations.JobOperator;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Step;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchLogger;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class StepExecutionRunner extends AbstractRunner implements Runnable {
    private Step step;
    private StepContextImpl stepContext;  //duplicate super.batchContext, for accessing StepContext-specific methods
    private Object stepResult;

    public StepExecutionRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner) {
        super(stepContext, enclosingRunner);
        this.step = stepContext.getStep();
        this.stepContext = stepContext;
    }

    @Override
    public void run() {
        LinkedList<Step> executedSteps = stepContext.getJobContext().getExecutedSteps();
        if (executedSteps.contains(step)) {
            StringBuilder stepIds = new StringBuilder();
            int i = 0, j = executedSteps.size() - 1;
            for (Iterator<Step> it = executedSteps.iterator(); it.hasNext(); i++) {
                stepIds.append(it.next().getId());
                if (i < j) {
                    stepIds.append(" -> ");
                }
            }
            throw LOGGER.loopbackStep(step.getId(), stepIds.toString());
        }

        stepContext.setBatchStatus(JobOperator.BatchStatus.STARTED);
        stepContext.getJobContext().setBatchStatus(JobOperator.BatchStatus.STARTED);

        try {
            Chunk chunk = step.getChunk();
            Batchlet batchlet = step.getBatchlet();
            if (chunk == null && batchlet == null) {
                stepContext.setBatchStatus(JobOperator.BatchStatus.ABANDONED);
                LOGGER.stepContainsNoChunkOrBatchlet(id);
                return;
            }

            if (chunk != null && batchlet != null) {
                stepContext.setBatchStatus(JobOperator.BatchStatus.ABANDONED);
                LOGGER.cannotContainBothChunkAndBatchlet(id);
                return;
            }

            for (StepListener l : stepContext.getStepListeners()) {
                try {
                    l.beforeStep();
                } catch (Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, l, "beforeStep");
                    stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                    return;
                }
            }


            BatchletRunner batchletRunner = new BatchletRunner(stepContext, enclosingRunner, batchlet);
            stepResult = batchletRunner.call();

            //TODO handle chunk type step


            //record the fact this step has been executed
            executedSteps.add(step);

            for (StepListener l : stepContext.getStepListeners()) {
                try {
                    l.afterStep();
                } catch (Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, l, "afterStep");
                    stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                    return;
                }
            }
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, step, "run");
            if (e instanceof Exception) {
                stepContext.setException((Exception) e);
            }
            stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
            for (AbstractContext c : stepContext.getOuterContexts()) {
                c.setBatchStatus(JobOperator.BatchStatus.FAILED);
            }
        }

        if (stepContext.getBatchStatus() == JobOperator.BatchStatus.STARTED) {  //has not been marked as failed, stopped or abandoned
            stepContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED);
            enclosingRunner.getBatchContext().setBatchStatus(JobOperator.BatchStatus.COMPLETED);  //set job batch status COMPLETED, may be changed next
        }

        if (stepContext.getBatchStatus() == JobOperator.BatchStatus.COMPLETED) {
            String next = step.getNext();
            if (next == null) {
                next = resolveControlElements(step.getControlElements());
            }
            enclosingRunner.runJobElement(next, stepContext.getStepExecution());
        }
    }

}
