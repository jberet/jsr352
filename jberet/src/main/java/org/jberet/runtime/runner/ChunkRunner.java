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

package org.jberet.runtime.runner;

import java.io.Serializable;
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
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.transaction.UserTransaction;

import org.jberet.job.Chunk;
import org.jberet.job.Collector;
import org.jberet.job.Listener;
import org.jberet.job.Listeners;
import org.jberet.metadata.ExceptionClassFilterImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.runtime.metric.StepMetrics;

import static org.jberet.util.BatchLogger.LOGGER;

public final class ChunkRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    private List<ChunkListener> chunkListeners = new ArrayList<ChunkListener>();

    private List<SkipWriteListener> skipWriteListeners = new ArrayList<SkipWriteListener>();
    private List<SkipProcessListener> skipProcessListeners = new ArrayList<SkipProcessListener>();
    private List<SkipReadListener> skipReadListeners = new ArrayList<SkipReadListener>();

    private List<RetryReadListener> retryReadListeners = new ArrayList<RetryReadListener>();
    private List<RetryWriteListener> retryWriteListeners = new ArrayList<RetryWriteListener>();
    private List<RetryProcessListener> retryProcessListeners = new ArrayList<RetryProcessListener>();

    private List<ItemReadListener> itemReadListeners = new ArrayList<ItemReadListener>();
    private List<ItemWriteListener> itemWriteListeners = new ArrayList<ItemWriteListener>();
    private List<ItemProcessListener> itemProcessListeners = new ArrayList<ItemProcessListener>();

    private JobContextImpl jobContext;
    private Chunk chunk;
    private StepExecutionRunner stepRunner;
    private StepMetrics stepMetrics;
    private ItemReader itemReader;
    private ItemWriter itemWriter;
    private ItemProcessor itemProcessor;
    private PartitionCollector collector;

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

    private Object itemRead;
    private List<Object> outputList = new ArrayList<Object>();

    private UserTransaction ut;

    public ChunkRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner, StepExecutionRunner stepRunner, Chunk chunk) {
        super(stepContext, enclosingRunner);
        this.stepRunner = stepRunner;
        this.stepMetrics = stepRunner.batchContext.getStepExecution().getStepMetrics();
        this.chunk = chunk;
        this.jobContext = batchContext.getJobContext();

        org.jberet.job.ItemReader readerElement = chunk.getReader();
        itemReader = jobContext.createArtifact(readerElement.getRef(), null, readerElement.getProperties(), batchContext);

        org.jberet.job.ItemWriter writerElement = chunk.getWriter();
        itemWriter = jobContext.createArtifact(writerElement.getRef(), null, writerElement.getProperties(), batchContext);

        org.jberet.job.ItemProcessor processorElement = chunk.getProcessor();
        if (processorElement != null) {
            itemProcessor = jobContext.createArtifact(processorElement.getRef(), null, processorElement.getProperties(), batchContext);
        }

        if (stepRunner.collectorDataQueue != null) {
            Collector collectorConfig = batchContext.getStep().getPartition().getCollector();
            if (collectorConfig != null) {
                collector = jobContext.createArtifact(collectorConfig.getRef(), null, collectorConfig.getProperties(), batchContext);
            }
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
            org.jberet.job.CheckpointAlgorithm alg = chunk.getCheckpointAlgorithm();
            if (alg != null) {
                checkpointAlgorithm = jobContext.createArtifact(alg.getRef(), null, alg.getProperties(), batchContext);
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
        this.ut = stepRunner.ut;
        createChunkRelatedListeners();
    }

    @Override
    public void run() {
        try {
            ut.setTransactionTimeout(checkpointTimeout());
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
            //collect data at the end of the partition
            if (collector != null) {
                stepRunner.collectorDataQueue.put(collector.collectPartitionData());
            }
            ut.commit();
        } catch (Exception e) {
            batchContext.setException(e);
            LOGGER.failToRunJob(e, jobContext.getJobName(), batchContext.getStepName(), chunk);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        } finally {
            try {
                if (stepRunner.collectorDataQueue != null) {
                    stepRunner.collectorDataQueue.put(batchContext.getStepExecution());
                }
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    /**
     * The main read-process-write loop
     * @throws Exception
     */
    private void readProcessWriteItems() throws Exception {
        ProcessingInfo processingInfo = new ProcessingInfo();
        //if input has not been depleted, or even if depleted, but still need to retry the last item
        //if stopped, exit the loop
        while ((processingInfo.chunkState != ChunkState.JOB_STOPPED) &&

               (processingInfo.chunkState != ChunkState.DEPLETED ||
                processingInfo.itemState == ItemState.TO_RETRY_READ ||
                processingInfo.itemState == ItemState.TO_RETRY_PROCESS ||
                processingInfo.itemState == ItemState.TO_RETRY_WRITE)
               ) {
            try {
                //reset state for the next iteration
                switch (processingInfo.itemState) {
                    case TO_SKIP:
                        processingInfo.itemState = ItemState.RUNNING;
                        break;
                    case TO_RETRY_READ:
                        processingInfo.itemState = ItemState.RETRYING_READ;
                        break;
                    case TO_RETRY_PROCESS:
                        processingInfo.itemState = ItemState.RETRYING_PROCESS;
                        break;
                    case TO_RETRY_WRITE:
                        processingInfo.itemState = ItemState.RETRYING_WRITE;
                        break;
                }

                if (processingInfo.chunkState == ChunkState.TO_START_NEW ||
                        processingInfo.chunkState == ChunkState.TO_RETRY ||
                        processingInfo.chunkState == ChunkState.TO_END_RETRY) {
                    if (processingInfo.chunkState == ChunkState.TO_START_NEW || processingInfo.chunkState == ChunkState.TO_END_RETRY) {
                        processingInfo.reset();
                    }
                    ut.begin();
                    for (ChunkListener l : chunkListeners) {
                        l.beforeChunk();
                    }
                    beginCheckpoint(processingInfo);
                }

                if (processingInfo.itemState != ItemState.RETRYING_PROCESS && processingInfo.itemState != ItemState.RETRYING_WRITE) {
                    readItem(processingInfo);
                }

                if (itemRead != null && processingInfo.itemState!= ItemState.RETRYING_WRITE) {
                    processItem(processingInfo);
                }

                if (processingInfo.toStopItem()) {
                    continue;
                }

                if (isReadyToCheckpoint(processingInfo)) {
                    try {
                        doCheckpoint(processingInfo);
                        for (ChunkListener l : chunkListeners) {
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
            } catch (Exception e) {
                for (ChunkListener l : chunkListeners) {
                    l.onError(e);
                }
                throw e;
            }
        }
    }

    private void readItem(final ProcessingInfo processingInfo) throws Exception {
        try {
            for (ItemReadListener l : itemReadListeners) {
                l.beforeRead();
            }
            itemRead = itemReader.readItem();
            if (itemRead != null) {  //only count successful read
                stepMetrics.increment(Metric.MetricType.READ_COUNT, 1);
                processingInfo.count++;
            } else {
                processingInfo.chunkState = ChunkState.DEPLETED;
            }
            for (ItemReadListener l : itemReadListeners) {
                l.afterRead(itemRead);
            }
        } catch (Exception e) {
            for (ItemReadListener l : itemReadListeners) {
                l.onReadError(e);
            }
            toSkipOrRetry(e, processingInfo);
            if (processingInfo.itemState == ItemState.TO_SKIP) {
                for (SkipReadListener l : skipReadListeners) {
                    l.onSkipReadItem(e);
                }
                stepMetrics.increment(Metric.MetricType.READ_SKIP_COUNT, 1);
                skipCount++;
                itemRead = null;
            } else if (processingInfo.itemState == ItemState.TO_RETRY) {
                for (RetryReadListener l : retryReadListeners) {
                    l.onRetryReadException(e);
                }
                retryCount++;
                if (needRollbackBeforeRetry(e)) {
                    rollbackCheckpoint(processingInfo);
                } else {
                    processingInfo.itemState = ItemState.TO_RETRY_READ;
                }
                itemRead = null;
            } else {
                throw e;
            }
            checkIfEndRetry(processingInfo, itemReader.checkpointInfo());
            if (processingInfo.itemState == ItemState.RETRYING_READ) {
                processingInfo.itemState = ItemState.RUNNING;
            }
        }
    }

    private void processItem(final ProcessingInfo processingInfo) throws Exception {
        Object output;
        if (itemProcessor != null) {
            try {
                for (ItemProcessListener l : itemProcessListeners) {
                    l.beforeProcess(itemRead);
                }
                output = itemProcessor.processItem(itemRead);
                for (ItemProcessListener l : itemProcessListeners) {
                    l.afterProcess(itemRead, output);
                }
                if (output == null) {
                    stepMetrics.increment(Metric.MetricType.FILTER_COUNT, 1);
                }
            } catch (Exception e) {
                for (ItemProcessListener l : itemProcessListeners) {
                    l.onProcessError(itemRead, e);
                }
                toSkipOrRetry(e, processingInfo);
                if (processingInfo.itemState == ItemState.TO_SKIP) {
                    for (SkipProcessListener l : skipProcessListeners) {
                        l.onSkipProcessItem(itemRead, e);
                    }
                    stepMetrics.increment(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
                    skipCount++;
                    output = null;
                } else if (processingInfo.itemState == ItemState.TO_RETRY) {
                    for (RetryProcessListener l : retryProcessListeners) {
                        l.onRetryProcessException(itemRead, e);
                    }
                    retryCount++;
                    if (needRollbackBeforeRetry(e)) {
                        rollbackCheckpoint(processingInfo);
                    } else {
                        processingInfo.itemState = ItemState.TO_RETRY_PROCESS;
                    }
                    output = null;
                } else {
                    throw e;
                }
            }
        } else {
            output = itemRead;
        }
        //a normal processing can also return null to exclude the processing result from writer.
        if (output != null) {
            outputList.add(output);
        }
        if (processingInfo.itemState != ItemState.TO_RETRY_PROCESS) {
            itemRead = null;
        }
        checkIfEndRetry(processingInfo, itemReader.checkpointInfo());
        if (processingInfo.itemState == ItemState.RETRYING_PROCESS) {
            processingInfo.itemState=ItemState.RUNNING;
        }
    }

    private void checkIfEndRetry(ProcessingInfo processingInfo, Serializable currentPosition) {
        if (processingInfo.chunkState == ChunkState.RETRYING &&
                processingInfo.itemState != ItemState.TO_RETRY_READ &&
                processingInfo.itemState != ItemState.TO_RETRY_PROCESS &&
                processingInfo.itemState != ItemState.TO_RETRY_WRITE &&
                processingInfo.failurePoint.equals(currentPosition)) {
            //if failurePoint is null, should fail with NPE
            processingInfo.chunkState = ChunkState.TO_END_RETRY;
        }
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
        processingInfo.chunkState = (processingInfo.chunkState == ChunkState.TO_RETRY) ?
                ChunkState.RETRYING : ChunkState.RUNNING;
    }

    private boolean isReadyToCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        if (jobContext.getJobExecution().isStopRequested()) {
            processingInfo.chunkState=ChunkState.JOB_STOPPING;
            return true;
        }
        if (processingInfo.chunkState == ChunkState.DEPLETED ||
                processingInfo.chunkState == ChunkState.RETRYING ||
                processingInfo.chunkState == ChunkState.TO_END_RETRY) {
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

    private void doCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        int outputSize = outputList.size();
        if (outputSize > 0) {
            try {
                for (ItemWriteListener l : itemWriteListeners) {
                    l.beforeWrite(outputList);
                }
                itemWriter.writeItems(outputList);
                stepMetrics.increment(Metric.MetricType.WRITE_COUNT, outputSize);
                for (ItemWriteListener l : itemWriteListeners) {
                    l.afterWrite(outputList);
                }
                batchContext.getStepExecution().setReaderCheckpointInfo(itemReader.checkpointInfo());
                batchContext.getStepExecution().setWriterCheckpointInfo(itemWriter.checkpointInfo());
                batchContext.savePersistentData();

                outputList.clear();
                if (!checkpointPolicy.equals("item")) {
                    checkpointAlgorithm.endCheckpoint();
                }
                if (processingInfo.chunkState == ChunkState.JOB_STOPPING) {
                    processingInfo.chunkState = ChunkState.JOB_STOPPED;
                } else if (processingInfo.chunkState != ChunkState.DEPLETED) {
                    processingInfo.chunkState = ChunkState.TO_START_NEW;
                }
                if (collector != null) {
                    stepRunner.collectorDataQueue.put(collector.collectPartitionData());
                }
            } catch (Exception e) {
                for (ItemWriteListener l : itemWriteListeners) {
                    l.onWriteError(outputList, e);
                }
                toSkipOrRetry(e, processingInfo);
                if (processingInfo.itemState == ItemState.TO_SKIP) {
                    //if requested to stop the job, do not skip to the next item
                    if (processingInfo.chunkState == ChunkState.JOB_STOPPING) {
                        processingInfo.chunkState = ChunkState.JOB_STOPPED;
                    } else if (processingInfo.chunkState != ChunkState.JOB_STOPPED) {
                        for (SkipWriteListener l : skipWriteListeners) {
                            l.onSkipWriteItem(outputList, e);
                        }
                        stepMetrics.increment(Metric.MetricType.WRITE_SKIP_COUNT, 1);
                        skipCount += outputSize;
                    }
                } else if (processingInfo.itemState == ItemState.TO_RETRY) {
                    for (RetryWriteListener l : retryWriteListeners) {
                        l.onRetryWriteException(outputList, e);
                    }
                    retryCount++;
                    if (needRollbackBeforeRetry(e)) {
                        rollbackCheckpoint(processingInfo);
                    } else {
                        processingInfo.itemState = ItemState.TO_RETRY_WRITE;
                    }
                } else {
                    throw e;
                }
            }
        }
        checkIfEndRetry(processingInfo, itemReader.checkpointInfo());
        if (processingInfo.itemState == ItemState.RETRYING_WRITE) {
            processingInfo.itemState = ItemState.RUNNING;
        }
    }

    private void rollbackCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        outputList.clear();
        processingInfo.failurePoint = itemReader.checkpointInfo();
        ut.rollback();
        stepMetrics.increment(Metric.MetricType.ROLLBACK_COUNT, 1);
        itemReader.close();
        itemWriter.close();
        itemReader.open(batchContext.getStepExecution().getReaderCheckpointInfo());
        itemWriter.open(batchContext.getStepExecution().getWriterCheckpointInfo());
        processingInfo.chunkState = ChunkState.TO_RETRY;
        processingInfo.itemState = ItemState.RUNNING;
        if (collector != null) {
            stepRunner.collectorDataQueue.put(collector.collectPartitionData());
        }
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

    private void toSkipOrRetry(Exception e, ProcessingInfo processingInfo) {
        if (processingInfo.chunkState == ChunkState.RETRYING ||
                processingInfo.chunkState == ChunkState.TO_END_RETRY ||
                processingInfo.itemState == ItemState.RETRYING_READ ||
                processingInfo.itemState == ItemState.RETRYING_PROCESS ||
                processingInfo.itemState == ItemState.RETRYING_WRITE) {
            //during retry, skip has precedence over retry
            if (needSkip(e)) {
                processingInfo.itemState = ItemState.TO_SKIP;
                return;
            } else if (needRetry(e)) {
                processingInfo.itemState = ItemState.TO_RETRY;
                return;
            }
        } else {
            //during normal processing, retry has precedence over skip
            if (needRetry(e)) {
                processingInfo.itemState = ItemState.TO_RETRY;
                return;
            } else if (needSkip(e)) {
                processingInfo.itemState = ItemState.TO_SKIP;
                return;
            }
        }
    }

    //already know need to retry, call this method to check if need to rollback before retry the current chunk
    private boolean needRollbackBeforeRetry(Exception e) {
        //if no-rollback-exceptions not configured, by default need to rollback the current chunk
        //else if the current exception does not match the configured no-rollback-exceptions, need to rollback
        return noRollbackExceptionClasses == null ||
                !noRollbackExceptionClasses.matches(e.getClass());
    }

    private void createChunkRelatedListeners() {
        Listeners listeners = batchContext.getStep().getListeners();
        if (listeners != null) {
            String ref;
            Object o;
            for (Listener l : listeners.getListener()) {
                ref = l.getRef();
                Class<?> cls = null;
                if (stepRunner.chunkRelatedListeners != null) {
                    cls = stepRunner.chunkRelatedListeners.get(ref);
                }
                o = jobContext.createArtifact(ref, cls, l.getProperties(), batchContext);
                if (o instanceof ChunkListener) {
                    chunkListeners.add((ChunkListener) o);
                }
                if (o instanceof ItemReadListener) {
                    itemReadListeners.add((ItemReadListener) o);
                }
                if (o instanceof ItemWriteListener) {
                    itemWriteListeners.add((ItemWriteListener) o);
                }
                if (o instanceof ItemProcessListener) {
                    itemProcessListeners.add((ItemProcessListener) o);
                }
                if (o instanceof SkipReadListener) {
                    skipReadListeners.add((SkipReadListener) o);
                }
                if (o instanceof SkipWriteListener) {
                    skipWriteListeners.add((SkipWriteListener) o);
                }
                if (o instanceof SkipProcessListener) {
                    skipProcessListeners.add((SkipProcessListener) o);
                }
                if (o instanceof RetryReadListener) {
                    retryReadListeners.add((RetryReadListener) o);
                }
                if (o instanceof RetryWriteListener) {
                    retryWriteListeners.add((RetryWriteListener) o);
                }
                if (o instanceof RetryProcessListener) {
                    retryProcessListeners.add((RetryProcessListener) o);
                }
            }
        }
    }

    private static final class ProcessingInfo {
        int count;
        boolean timerExpired;
        ItemState itemState = ItemState.RUNNING;
        ChunkState chunkState = ChunkState.TO_START_NEW;

        /**
         * Where the failure occurred that caused the current retry.  The retry should stop after the item at failurePoint
         * has been retried.
         */
        Serializable failurePoint;

        private void reset() {
            count = 0;
            timerExpired = false;
            itemState = ItemState.RUNNING;
            chunkState = ChunkState.RUNNING;
            failurePoint = null;
        }

        private boolean toStopItem() {
            return itemState == ItemState.TO_SKIP || itemState == ItemState.TO_RETRY ||
                    itemState == ItemState.TO_RETRY_READ || itemState == ItemState.TO_RETRY_PROCESS ||
                    itemState == ItemState.TO_RETRY_WRITE;
        }
    }

    private enum ItemState {
        RUNNING,       //normal item processing
        TO_SKIP,        //need to skip the remainder of the current iteration
        TO_RETRY,       //a super-type value for TO_RETRY_*, used to indicate whether to skip or retry current item

        TO_RETRY_READ,  //need to retry the current item read operation, upon starting next iteration => RETRYING_READ
        RETRYING_READ,  //the current item is being re-read, when successful or result in a skip => normal RUNNING

        TO_RETRY_PROCESS, //need to retry the current item process operation, upon starting next iteration => RETRYING_PROCESS
        RETRYING_PROCESS, //the current item is being re-processed, when successful or result in a skip => normal RUNNING

        TO_RETRY_WRITE, //need to retry the current item write operation, upon starting next items => RETRYING_WRITE
        RETRYING_WRITE,  //the current item is being re-written, when successful or result in a skip => normal RUNNING
    }

    private enum ChunkState {
        RUNNING,      //normal running of chunk
        TO_RETRY,     //need to retry the current chunk
        RETRYING,     //chunk being retried
        TO_END_RETRY, //need to end retrying the current chunk
        TO_START_NEW, //the current chunk is done and need to start a new chunk next
        DEPLETED,      //no more input items, the processing can still go to next iteration so this last item can be retried

        JOB_STOPPING,  //the job has been requested to stop
        JOB_STOPPED
        }

}
