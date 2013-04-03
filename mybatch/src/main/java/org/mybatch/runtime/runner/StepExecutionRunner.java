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
import java.util.LinkedList;
import java.util.List;
import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.listener.StepListener;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Step;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchLogger;
import org.mybatch.util.BatchUtil;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class StepExecutionRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    private Step step;
    private Object stepResult;

    List<StepListener> stepListeners = new ArrayList<StepListener>();
    List<ChunkListener> chunkListeners = new ArrayList<ChunkListener>();

    List<SkipWriteListener> skipWriteListeners = new ArrayList<SkipWriteListener>();
    List<SkipProcessListener> skipProcessListeners = new ArrayList<SkipProcessListener>();
    List<SkipReadListener> skipReadListeners = new ArrayList<SkipReadListener>();

    List<RetryReadListener> retryReadListeners = new ArrayList<RetryReadListener>();
    List<RetryWriteListener> retryWriteListeners = new ArrayList<RetryWriteListener>();
    List<RetryProcessListener> retryProcessListeners = new ArrayList<RetryProcessListener>();

    List<ItemReadListener> itemReadListeners = new ArrayList<ItemReadListener>();
    List<ItemWriteListener> itemWriteListeners = new ArrayList<ItemWriteListener>();
    List<ItemProcessListener> itemProcessListeners = new ArrayList<ItemProcessListener>();


    public StepExecutionRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner) {
        super(stepContext, enclosingRunner);
        this.step = stepContext.getStep();
        createStepListeners();
    }

    @Override
    public void run() {
        Boolean allowStartIfComplete = batchContext.getAllowStartIfComplete();
        if (allowStartIfComplete != Boolean.FALSE) {
            try {
                LinkedList<Step> executedSteps = batchContext.getJobContext().getExecutedSteps();
                if (executedSteps.contains(step)) {
                    StringBuilder stepIds = BatchUtil.toElementSequence(executedSteps);
                    stepIds.append(step.getId());
                    throw LOGGER.loopbackStep(step.getId(), stepIds.toString());
                }


                int startLimit = 0;
                if (step.getStartLimit() != null) {
                    startLimit = Integer.parseInt(step.getStartLimit());
                }
                if (startLimit > 0) {
                    int startCount = batchContext.getStepExecution().getStartCount();
                    if (startCount >= startLimit) {
                        throw LOGGER.stepReachedStartLimit(step.getId(), startLimit, startCount);
                    }
                }

                batchContext.getStepExecution().incrementStartCount();
                batchContext.setBatchStatus(BatchStatus.STARTED);
                batchContext.getJobContext().getJobExecution().addStepExecution(batchContext.getStepExecution());

                Chunk chunk = step.getChunk();
                Batchlet batchlet = step.getBatchlet();
                if (chunk == null && batchlet == null) {
                    batchContext.setBatchStatus(BatchStatus.ABANDONED);
                    LOGGER.stepContainsNoChunkOrBatchlet(id);
                    return;
                }

                if (chunk != null && batchlet != null) {
                    batchContext.setBatchStatus(BatchStatus.ABANDONED);
                    LOGGER.cannotContainBothChunkAndBatchlet(id);
                    return;
                }

                for (StepListener l : stepListeners) {
                    l.beforeStep();
                }

                if (batchlet != null) {
                    BatchletRunner batchletRunner = new BatchletRunner(batchContext, enclosingRunner, batchlet);
                    stepResult = batchletRunner.call();
                } else {
                    ChunkRunner chunkRunner = new ChunkRunner(batchContext, enclosingRunner, this, chunk);
                    chunkRunner.run();
                }

                //record the fact this step has been executed
                executedSteps.add(step);

                for (StepListener l : stepListeners) {
                    try {
                        l.afterStep();
                    } catch (Throwable e) {
                        BatchLogger.LOGGER.failToRunJob(e, "", l);
                        batchContext.setBatchStatus(BatchStatus.FAILED);
                        return;
                    }
                }
            } catch (Throwable e) {
                LOGGER.failToRunJob(e, step.getId(), step);
                if (e instanceof Exception) {
                    batchContext.setException((Exception) e);
                } else {
                    batchContext.setException(new BatchRuntimeException(e));
                }
                batchContext.setBatchStatus(BatchStatus.FAILED);
            }

            BatchStatus stepStatus = batchContext.getBatchStatus();
            switch (stepStatus) {
                case STARTED:
                    batchContext.setBatchStatus(BatchStatus.COMPLETED);
                    break;
                case FAILED:
                    for (AbstractContext e : batchContext.getOuterContexts()) {
                        e.setBatchStatus(BatchStatus.FAILED);
                    }
                    break;
            }
        }

        if (batchContext.getBatchStatus() == BatchStatus.COMPLETED) {
            String next = resolveTransitionElements(step.getTransitionElements(), step.getNext());
            enclosingRunner.runJobElement(next, batchContext.getStepExecution());
        }
    }

    private void createStepListeners() {
        Listeners listeners = step.getListeners();
        if (listeners != null) {
            for (Listener listener : listeners.getListener()) {
                //ask the root JobContext to create artifact
                Object o = batchContext.getJobContext().createArtifact(listener.getRef(), listener.getProperties(), batchContext);
                if (o instanceof StepListener) {
                    stepListeners.add((StepListener) o);
                } else if (o instanceof ChunkListener) {
                    chunkListeners.add((ChunkListener) o);
                } else if (o instanceof SkipWriteListener) {
                    skipWriteListeners.add((SkipWriteListener) o);
                } else if (o instanceof SkipProcessListener) {
                    skipProcessListeners.add((SkipProcessListener) o);
                } else if (o instanceof SkipReadListener) {
                    skipReadListeners.add((SkipReadListener) o);
                } else if (o instanceof RetryReadListener) {
                    retryReadListeners.add((RetryReadListener) o);
                } else if (o instanceof RetryWriteListener) {
                    retryWriteListeners.add((RetryWriteListener) o);
                } else if (o instanceof RetryProcessListener) {
                    retryProcessListeners.add((RetryProcessListener) o);
                } else if (o instanceof ItemReadListener) {
                    itemReadListeners.add((ItemReadListener) o);
                } else if (o instanceof ItemWriteListener) {
                    itemWriteListeners.add((ItemWriteListener) o);
                } else if (o instanceof ItemProcessListener) {
                    itemProcessListeners.add((ItemProcessListener) o);
                }
            }
        }
    }

}
