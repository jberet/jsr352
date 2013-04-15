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
import java.util.Timer;
import java.util.TimerTask;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.transaction.UserTransaction;

import org.mybatch.job.Chunk;
import org.mybatch.metadata.ExceptionClassFilterImpl;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.runtime.metric.StepMetrics;
import org.mybatch.util.TransactionService;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class ChunkRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    private Chunk chunk;
    private StepExecutionRunner stepRunner;
    private StepMetrics stepMetrics;
    private ItemReader itemReader;
    private ItemWriter itemWriter;
    private ItemProcessor itemProcessor;
    private String checkpointPolicy = "item";
    private CheckpointAlgorithm checkpointAlgorithm;
    private int itemCount = 10;
    private int timeLimit;  //in seconds
    private int skipLimit;  //default no limit
    private int retryLimit;  //default no limit

    private ExceptionClassFilterImpl skippableExceptionClasses;
    private ExceptionClassFilterImpl retryableExceptionClasses;
    private ExceptionClassFilterImpl noRollbackExceptionClasses;
    private int skipCount;
    private int retryCount;

    private UserTransaction ut;

    public ChunkRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner, StepExecutionRunner stepRunner, Chunk chunk) {
        super(stepContext, enclosingRunner);
        this.stepRunner = stepRunner;
        this.stepMetrics = stepRunner.batchContext.getStepExecution().getStepMetrics();
        this.chunk = chunk;

        org.mybatch.job.ItemReader readerElement = chunk.getReader();
        itemReader = batchContext.getJobContext().createArtifact(
                readerElement.getRef(), readerElement.getProperties(), batchContext);

        org.mybatch.job.ItemWriter writerElement = chunk.getWriter();
        itemWriter = batchContext.getJobContext().createArtifact(
                writerElement.getRef(), writerElement.getProperties(), batchContext);

        org.mybatch.job.ItemProcessor processorElement = chunk.getProcessor();
        if (processorElement != null) {
            itemProcessor = batchContext.getJobContext().createArtifact(
                    processorElement.getRef(), processorElement.getProperties(), batchContext);
        }

        String attrVal = chunk.getCheckpointPolicy();
        if (attrVal == null || attrVal.equals("item")) {
            attrVal = chunk.getItemCount();
            if (attrVal != null) {
                itemCount = Integer.parseInt(attrVal);
                if (itemCount < 1) {
                    throw LOGGER.invalidItemCount(itemCount);
                }
            }
            attrVal = chunk.getTimeLimit();
            if (attrVal != null) {
                timeLimit = Integer.parseInt(attrVal);
            }
        } else if (attrVal.equals("custom")) {
            checkpointPolicy = "custom";
            org.mybatch.job.CheckpointAlgorithm alg = chunk.getCheckpointAlgorithm();
            if (alg != null) {
                checkpointAlgorithm = batchContext.getJobContext().createArtifact(
                        alg.getRef(), alg.getProperties(), batchContext);
            } else {
                throw LOGGER.checkpointAlgorithmMissing(stepRunner.step.getId());
            }
        } else {
            throw LOGGER.invalidCheckpointPolicy(attrVal);
        }

        attrVal = chunk.getSkipLimit();
        if (attrVal != null) {
            skipLimit = Integer.parseInt(attrVal);
        }
        attrVal = chunk.getRetryLimit();
        if (attrVal != null) {
            retryLimit = Integer.parseInt(attrVal);
        }

        skippableExceptionClasses = (ExceptionClassFilterImpl) chunk.getSkippableExceptionClasses();
        retryableExceptionClasses = (ExceptionClassFilterImpl) chunk.getRetryableExceptionClasses();
        noRollbackExceptionClasses = (ExceptionClassFilterImpl) chunk.getNoRollbackExceptionClasses();
        ut = TransactionService.getTransaction();
    }

    @Override
    public void run() {
        try {
            ut.begin();
            try {
                itemReader.open(batchContext.getStepExecution().getReaderCheckpointInfo());
                itemWriter.open(batchContext.getStepExecution().getWriterCheckpointInfo());
            } catch (Exception e) {
                ut.rollback();
                throw e;
            }
            ut.commit();

            readProcessWriteItems();

            ut.begin();
            try {
                itemReader.close();
                itemWriter.close();
            } catch (Exception e) {
                ut.rollback();
                throw e;
            }
            ut.commit();
        } catch (Throwable e) {
            Exception exception = e instanceof Exception ? (Exception) e : new BatchRuntimeException(e);
            batchContext.setException(exception);
            LOGGER.failToRunJob(e, batchContext.getJobContext().getJobName(), batchContext.getStepName(), chunk);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        }
    }

    private void readProcessWriteItems() throws Exception {
        List<Object> outputList = new ArrayList<Object>();
        ProcessingInfo processingInfo = new ProcessingInfo();
        Object item = null;
        while (!processingInfo.depleted) {
            try {
                if (processingInfo.startingNewChunk) {
                    processingInfo = new ProcessingInfo();
                    ut.setTransactionTimeout(checkpointTimeout());
                    ut.begin();
                    for (ChunkListener l : stepRunner.chunkListeners) {
                        l.beforeChunk();
                    }
                    beginCheckpoint(processingInfo);
                }
                item = readItem(processingInfo);
                if (item != null) {
                    processItem(item, processingInfo, outputList);
                }

                if (!processingInfo.skipThisItem) {
                    if (isReadyToCheckpoint(processingInfo)) {
                        ut.begin();
                        try {
                            doCheckpoint(processingInfo, outputList);
                            for (ChunkListener l : stepRunner.chunkListeners) {
                                l.afterChunk();
                            }
                        } catch (Exception e) {
                            ut.rollback();
                            stepMetrics.increment(Metric.MetricType.ROLLBACK_COUNT, 1);
                            throw e;
                        }
                        ut.commit();
                        stepMetrics.increment(Metric.MetricType.COMMIT_COUNT, 1);
                    }
                } else {
                    processingInfo.skipThisItem = false;  //reset for the next iteration
                }
            } catch (Exception e) {
                for (ChunkListener l : stepRunner.chunkListeners) {
                    l.onError(e);
                }
                throw e;
            }
        }
    }

    private Object readItem(final ProcessingInfo processingInfo) throws Exception {
        Object result = null;
        try {
            for (ItemReadListener l : stepRunner.itemReadListeners) {
                l.beforeRead();
            }
            result = itemReader.readItem();
            if (result != null) {  //only count successful read
                stepMetrics.increment(Metric.MetricType.READ_COUNT, 1);
            } else {
                processingInfo.depleted = true;
            }
            for (ItemReadListener l : stepRunner.itemReadListeners) {
                l.afterRead(result);
            }
        } catch (Exception e) {
            for (ItemReadListener l : stepRunner.itemReadListeners) {
                l.onReadError(e);
            }
            if (needSkip(e)) {
                for (SkipReadListener l : stepRunner.skipReadListeners) {
                    l.onSkipReadItem(e);
                }
                stepMetrics.increment(Metric.MetricType.READ_SKIP_COUNT, 1);
                skipCount++;
                processingInfo.skipThisItem = true;
                return null;
            } else {
                throw e;
            }
        }
        return result;
    }

    private void processItem(final Object item, final ProcessingInfo processingInfo, final List<Object> outputList) throws Exception {
        Object output;
        if (itemProcessor != null) {
            try {
                for (ItemProcessListener l : stepRunner.itemProcessListeners) {
                    l.beforeProcess(item);
                }
                output = itemProcessor.processItem(item);
                for (ItemProcessListener l : stepRunner.itemProcessListeners) {
                    l.afterProcess(item, output);
                }
                if (output == null) {
                    stepMetrics.increment(Metric.MetricType.FILTER_COUNT, 1);
                }
            } catch (Exception e) {
                for (ItemProcessListener l : stepRunner.itemProcessListeners) {
                    l.onProcessError(item, e);
                }
                if (needSkip(e)) {
                    for (SkipProcessListener l : stepRunner.skipProcessListeners) {
                        l.onSkipProcessItem(item, e);
                    }
                    stepMetrics.increment(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
                    skipCount++;
                    output = null;
                    processingInfo.skipThisItem = true;
                } else {
                    throw e;
                }
            }
        } else {
            output = item;
        }
        //a normal processing can also return null to exclude the processing result from writer.
        if (output != null) {
            outputList.add(output);
        }
        processingInfo.count++;
    }

    private int checkpointTimeout() throws Exception {
        if (checkpointPolicy.equals("item")) {
            return 0;  //0 indicates jta system default
        } else {
            return checkpointAlgorithm.checkpointTimeout();
        }
    }

    private void beginCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        if (checkpointPolicy.equals("item")) {
            if (timeLimit > 0) {
                Timer timer = new Timer("chunk-checkpoint-timer", true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        processingInfo.timerExpired = true;
                    }
                }, timeLimit * 1000);
            }
        } else {
            checkpointAlgorithm.beginCheckpoint();
        }
        processingInfo.startingNewChunk = false;
    }

    private boolean isReadyToCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        if (processingInfo.depleted) {
            return true;
        }
        if (checkpointPolicy.equals("item")) {
            if (processingInfo.count >= itemCount) {
                return true;
            }
            if (timeLimit > 0) {
                return processingInfo.timerExpired;
            }
            return false;
        }
        return checkpointAlgorithm.isReadyToCheckpoint();
    }

    private void doCheckpoint(final ProcessingInfo processingInfo, final List<Object> outputList) throws Exception {
        int outputSize = outputList.size();
        if (outputSize > 0) {
            try {
                for (ItemWriteListener l : stepRunner.itemWriteListeners) {
                    l.beforeWrite(outputList);
                }
                itemWriter.writeItems(outputList);
                stepMetrics.increment(Metric.MetricType.WRITE_COUNT, outputSize);
                for (ItemWriteListener l : stepRunner.itemWriteListeners) {
                    l.afterWrite(outputList);
                }
                batchContext.getStepExecution().setReaderCheckpointInfo(itemReader.checkpointInfo());
                batchContext.getStepExecution().setWriterCheckpointInfo(itemWriter.checkpointInfo());
                batchContext.savePersistentData();
            } catch (Exception e) {
                for (ItemWriteListener l : stepRunner.itemWriteListeners) {
                    l.onWriteError(outputList, e);
                }
                if (needSkip(e)) {
                    for (SkipWriteListener l : stepRunner.skipWriteListeners) {
                        l.onSkipWriteItem(outputList, e);
                    }
                    stepMetrics.increment(Metric.MetricType.WRITE_SKIP_COUNT, 1);
                    skipCount += outputSize;
                } else {
                    throw e;
                }
            }
            outputList.clear();
        }
        if (!checkpointPolicy.equals("item")) {
            checkpointAlgorithm.endCheckpoint();
        }
        processingInfo.startingNewChunk = true;
        processingInfo.count = 0;
    }

    private boolean needSkip(Exception e) {
        return skippableExceptionClasses != null &&
                ((skipLimit >= 0 && skipCount < skipLimit) || skipLimit < 0) &&
                skippableExceptionClasses.matches(e.getClass());
    }

    private boolean needRetry(Exception e) {
        return retryableExceptionClasses != null &&
                ((retryLimit >= 0 && retryCount < retryLimit) || retryLimit < 0) &&
                retryableExceptionClasses.matches(e.getClass());
    }

    //already called needRetry(Exception) and returned true, then call this method to check if need to rollback
    //before retry the current chunk
    private boolean needRollbackBeforeRetry(Exception e) {
        return noRollbackExceptionClasses == null ||
                noRollbackExceptionClasses.matches(e.getClass());
    }

    private static final class ProcessingInfo {
        int count;
        boolean timerExpired;
        boolean startingNewChunk = true;
        boolean skipThisItem;
        boolean depleted;
    }
}
