/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.runner;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

import org.jberet.job.model.Chunk;
import org.jberet.job.model.Step;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.PartitionHandler;
import org.jberet.spi.PartitionWorker;

public class ThreadPartitionHandler implements PartitionHandler {
    private StepExecutionRunner stepExecutionRunner;

    private BlockingQueue<Boolean> completedPartitionThreads;

    private BlockingQueue<Serializable> collectorDataQueue;

    public ThreadPartitionHandler(final StepExecutionRunner stepExecutionRunner) {
        this.stepExecutionRunner = stepExecutionRunner;
    }

    @Override
    public void setResourceTracker(final BlockingQueue<Boolean> completedPartitionThreads) {
        this.completedPartitionThreads = completedPartitionThreads;
    }

    @Override
    public void setCollectorDataQueue(final BlockingQueue<Serializable> collectorDataQueue) {
        this.collectorDataQueue = collectorDataQueue;
    }

    @Override
    public void submitPartitionTask(final StepContextImpl partitionStepContext,
                                    final int currentIndex,
                                    final int numOfPartitions) {
        final AbstractRunner<StepContextImpl> runner1;
        final Step step1 = partitionStepContext.getStep();
        final Chunk ch = step1.getChunk();
        final PartitionWorker partitionWorker = new ThreadPartitionWorker(completedPartitionThreads, collectorDataQueue);
        if (ch == null) {
            runner1 = new BatchletRunner(partitionStepContext, stepExecutionRunner.enclosingRunner,
                    step1.getBatchlet(), partitionWorker);
        } else {
            runner1 = new ChunkRunner(partitionStepContext, stepExecutionRunner.enclosingRunner,
                    ch, stepExecutionRunner.tm, partitionWorker);
        }
        stepExecutionRunner.jobContext.getBatchEnvironment().submitTask(runner1);
    }
}
