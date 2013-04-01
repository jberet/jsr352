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
import java.util.List;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;

import org.mybatch.job.Chunk;
import org.mybatch.runtime.context.StepContextImpl;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class ChunkRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    private Chunk chunk;
    private StepExecutionRunner stepRunner;

    public ChunkRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner, StepExecutionRunner stepRunner, Chunk chunk) {
        super(stepContext, enclosingRunner);
        this.stepRunner = stepRunner;
        this.chunk = chunk;
    }

    @Override
    public void run() {
        ItemReader itemReader = null;
        ItemWriter itemWriter = null;
        ItemProcessor itemProcessor = null;
        try {
            for (ChunkListener l : stepRunner.chunkListeners) {
                l.beforeChunk();
            }

            org.mybatch.job.ItemReader readerElement = chunk.getReader();
            itemReader = (ItemReader) batchContext.getJobContext().createArtifact(
                    readerElement.getRef(), readerElement.getProperties(), batchContext);

            org.mybatch.job.ItemWriter writerElement = chunk.getWriter();
            itemWriter = (ItemWriter) batchContext.getJobContext().createArtifact(
                    writerElement.getRef(), writerElement.getProperties(), batchContext);

            org.mybatch.job.ItemProcessor processorElement = chunk.getProcessor();
            if (processorElement != null) {
                itemProcessor = (ItemProcessor) batchContext.getJobContext().createArtifact(
                        processorElement.getRef(), processorElement.getProperties(), batchContext);
            }
            itemReader.open(null);
            itemWriter.open(null);

            Object item = itemReader.readItem();
            List<Object> outputList = new ArrayList<Object>();
            while (item != null) {
                Object output = itemProcessor.processItem(item);
                outputList.add(output);
                itemWriter.writeItems(outputList);
            }

            itemReader.close();
            itemWriter.close();

            for (ChunkListener l : stepRunner.chunkListeners) {
                l.afterChunk();
            }
        } catch (Throwable e) {
            Exception exception = e instanceof Exception ? (Exception) e : new BatchRuntimeException(e);
            batchContext.setException(exception);
            for (ChunkListener l : stepRunner.chunkListeners) {
                try {
                    l.onError(exception);
                } catch (Exception e2) {
                    LOGGER.failToRunJob(e2, batchContext.getStepName(), l);
                }
            }

            LOGGER.failToRunJob(e, batchContext.getStepName(), chunk);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        }
    }

}
