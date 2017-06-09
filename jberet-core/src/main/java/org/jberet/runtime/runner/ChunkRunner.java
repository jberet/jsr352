/*
 * Copyright (c) 2012-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.runner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jberet._private.BatchLogger;
import org.jberet.creation.JobScopedContextImpl;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.ExceptionClassFilter;
import org.jberet.job.model.Listeners;
import org.jberet.job.model.Properties;
import org.jberet.job.model.RefArtifact;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.runtime.metric.StepMetrics;
import org.jberet.spi.JobTask;
import org.jberet.spi.PartitionWorker;
import org.jboss.logging.Logger;

import static org.jberet._private.BatchLogger.LOGGER;
import static org.jberet._private.BatchMessages.MESSAGES;

/**
 * This runner class is responsible for running a chunk-type step (not just a chunk range of a step).  In a partitioned
 * step execution, multiple ChunkRunner instances are created, one for each partition.  The StepContextImpl and
 * StepExecutionImpl associated with each ChunkRunner in a partition are cloned from the original counterparts.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ChunkRunner extends AbstractRunner<StepContextImpl> implements JobTask {
    private final List<Object> allChunkRelatedListeners = new ArrayList<Object>();
    private final List<ChunkListener> chunkListeners = new ArrayList<ChunkListener>();

    private final List<SkipWriteListener> skipWriteListeners = new ArrayList<SkipWriteListener>();
    private final List<SkipProcessListener> skipProcessListeners = new ArrayList<SkipProcessListener>();
    private final List<SkipReadListener> skipReadListeners = new ArrayList<SkipReadListener>();

    private final List<RetryReadListener> retryReadListeners = new ArrayList<RetryReadListener>();
    private final List<RetryWriteListener> retryWriteListeners = new ArrayList<RetryWriteListener>();
    private final List<RetryProcessListener> retryProcessListeners = new ArrayList<RetryProcessListener>();

    private final List<ItemReadListener> itemReadListeners = new ArrayList<ItemReadListener>();
    private final List<ItemWriteListener> itemWriteListeners = new ArrayList<ItemWriteListener>();
    private final List<ItemProcessListener> itemProcessListeners = new ArrayList<ItemProcessListener>();

    private final Chunk chunk;
    private final StepMetrics stepMetrics;
    private final AbstractStepExecution stepOrPartitionExecution;
    private ItemReader itemReader;
    private ItemWriter itemWriter;
    private ItemProcessor itemProcessor;
    private PartitionCollector collector;

    private String checkpointPolicy = "item";
    private CheckpointAlgorithm checkpointAlgorithm;
    private int itemCount = 10;
    private int timeLimit;  //in seconds
    private final int skipLimit;  //default no limit
    private final int retryLimit;  //default no limit

    private final ExceptionClassFilter skippableExceptionClasses;
    private final ExceptionClassFilter retryableExceptionClasses;
    private final ExceptionClassFilter noRollbackExceptionClasses;
    private int skipCount;
    private int retryCount;

    private Object itemRead;
    private final List<Object> outputList = new ArrayList<Object>();

    private final TransactionManager tm;
    private final AtomicBoolean itemReaderClosed = new AtomicBoolean(false);
    private final AtomicBoolean itemWriterClosed = new AtomicBoolean(false);

    private final PartitionWorker partitionWorker;

    public ChunkRunner(final StepContextImpl stepContext,
                       final CompositeExecutionRunner enclosingRunner,
                       final Chunk chunk,
                       final TransactionManager tm,
                       final PartitionWorker partitionWorker)  {
        super(stepContext, enclosingRunner);
        this.chunk = chunk;
        this.stepOrPartitionExecution = stepContext.getStepExecution();
        this.stepMetrics = this.stepOrPartitionExecution.getStepMetrics();
        this.partitionWorker = partitionWorker;

        String attrVal = chunk.getSkipLimit();
        skipLimit = attrVal == null ? -1 : Integer.parseInt(attrVal);

        attrVal = chunk.getRetryLimit();
        retryLimit = attrVal == null ? -1 : Integer.parseInt(attrVal);

        skippableExceptionClasses = chunk.getSkippableExceptionClasses();
        retryableExceptionClasses = chunk.getRetryableExceptionClasses();
        noRollbackExceptionClasses = chunk.getNoRollbackExceptionClasses();

        this.tm = tm != null ? tm :
                StepExecutionRunner.getTransactionManager(jobContext, stepContext.getStep());
    }

    @Override
    public void run() {
        try {
            itemReader = (ItemReader) createArtifact(chunk.getReader(), batchContext, ScriptItemReader.class);
            itemWriter = (ItemWriter) createArtifact(chunk.getWriter(), batchContext, ScriptItemWriter.class);

            final RefArtifact processorElement = chunk.getProcessor();
            if (processorElement != null) {
                itemProcessor = (ItemProcessor) createArtifact(processorElement, batchContext, ScriptItemProcessor.class);
            }

            if (partitionWorker != null) {
                final RefArtifact collectorConfig = batchContext.getStep().getPartition().getCollector();
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
                        throw MESSAGES.invalidItemCount(itemCount);
                    }
                }
                attrVal = chunk.getTimeLimit();
                if (attrVal != null) {
                    timeLimit = Integer.parseInt(attrVal);
                }
            } else if (attrVal.equals("custom")) {
                checkpointPolicy = "custom";
                final RefArtifact alg = chunk.getCheckpointAlgorithm();
                if (alg != null) {
                    checkpointAlgorithm = jobContext.createArtifact(alg.getRef(), null, alg.getProperties(), batchContext);
                } else {
                    throw MESSAGES.checkpointAlgorithmMissing(batchContext.getStep().getId());
                }
            } else {
                throw MESSAGES.invalidCheckpointPolicy(attrVal);
            }
            createChunkRelatedListeners();


            //When running in EE environment, set global transaction timeout for the current thread
            // from javax.transaction.global.timeout property at step level
            final Properties stepProps = batchContext.getStep().getProperties();
            int globalTimeout = 180; //default 180 seconds defined by spec
            if (stepProps != null) {
                final String globalTimeoutProp = stepProps.get("javax.transaction.global.timeout");
                if (globalTimeoutProp != null) {
                    globalTimeout = Integer.parseInt(globalTimeoutProp);
                }
            }
            tm.setTransactionTimeout(globalTimeout);
            tm.begin();
            try {
                itemReaderClosed.set(false);
                itemReader.open(stepOrPartitionExecution.getReaderCheckpointInfo());
                itemWriterClosed.set(false);
                itemWriter.open(stepOrPartitionExecution.getWriterCheckpointInfo());
                tm.commit();
            } catch (final Exception e) {
                tm.rollback();
                // An error occurred, safely close the reader and writer
                safeClose();
                throw e;
            }

            readProcessWriteItems();

            tm.begin();
            try {
                closeItemWriter();
                closeItemReader();
                tm.commit();
            } catch (final Exception e) {
                tm.rollback();
                // An error occurred, safely close the reader and writer
                safeClose();
                throw e;
            }
            //collect data at the end of the partition
            if (collector != null) {
                partitionWorker.reportData(collector.collectPartitionData(), stepOrPartitionExecution);
            }
            //set batch status to indicate that either the main step, or a partition has completed successfully.
            //note that when a chunk range is completed, we should not set batch status as completed.
            //make sure the step has not been set to STOPPED.
            if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
                batchContext.setBatchStatus(BatchStatus.COMPLETED);
            }
        } catch (final Throwable e) {
            batchContext.setException(e instanceof Exception ? (Exception) e : new BatchRuntimeException(e));
            LOGGER.log(Logger.Level.ERROR, "item-count=" + itemCount + ", time-limit=" + timeLimit +
                    ", skip-limit=" + skipLimit + ", skipCount=" + skipCount +
                    ", retry-limit=" + retryLimit + ", retryCount=" + retryCount);
            LOGGER.failToRunJob(e, jobContext.getJobName(), batchContext.getStepName(), chunk);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        } finally {
            if (partitionWorker != null) {
                try {
                    JobScopedContextImpl.ScopedInstance.destroy(batchContext.getPartitionScopedBeans());
                    partitionWorker.partitionDone(stepOrPartitionExecution);
                } catch (final Exception e) {
                    BatchLogger.LOGGER.problemFinalizingPartitionExecution(e, stepOrPartitionExecution.getStepExecutionId());
                }
            }

            jobContext.destroyArtifact(itemReader, itemWriter, itemProcessor, collector, checkpointAlgorithm);
            jobContext.destroyArtifact(allChunkRelatedListeners);
            // Safely close the reader and writer
            safeClose();
        }
    }

    /**
     * The main read-process-write loop
     *
     * @throws Exception
     */
    private void readProcessWriteItems() throws Exception {
        final ProcessingInfo processingInfo = new ProcessingInfo();
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
                        processingInfo.chunkState == ChunkState.RETRYING ||
                        processingInfo.chunkState == ChunkState.TO_END_RETRY) {
                    if (processingInfo.chunkState == ChunkState.TO_START_NEW || processingInfo.chunkState == ChunkState.TO_END_RETRY) {
                        processingInfo.reset();
                    }
                    //if during Chunk RETRYING, and an item is skipped, the ut is still active so no need to begin a new one
                    if (tm.getStatus() != Status.STATUS_ACTIVE) {
                        //other part of the app might have marked the transaction as rollback only, so roll it back
                        if (tm.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                            tm.rollback();
                        }
                        if (checkpointAlgorithm != null) {
                            tm.setTransactionTimeout(checkpointAlgorithm.checkpointTimeout());
                            checkpointAlgorithm.beginCheckpoint();
                        }
                        tm.begin();
                    }
                    for (final ChunkListener l : chunkListeners) {
                        l.beforeChunk();
                    }
                    beginCheckpoint(processingInfo);
                }

                if (processingInfo.itemState != ItemState.RETRYING_PROCESS && processingInfo.itemState != ItemState.RETRYING_WRITE) {
                    readItem(processingInfo);
                }

                if (itemRead != null && processingInfo.itemState != ItemState.RETRYING_WRITE) {
                    processItem(processingInfo);
                }

                if (processingInfo.toStopItem()) {
                    continue;
                }

                if (isReadyToCheckpoint(processingInfo)) {
                    doCheckpoint(processingInfo);

                    //errors may happen during the above doCheckpoint (e.g., in writer.write method).  If so, need
                    //to skip the remainder of the current loop.  If retry with rollback, chunkState has been set to
                    //TO_RETRY; if retry with no rollback, itemState has been set to TO_RETRY_WRITE; if skip,
                    //itemState has been set to TO_SKIP

                    // end of a chunk, so the following block is no longer needed
                    //if (processingInfo.chunkState == ChunkState.TO_RETRY || processingInfo.itemState == ItemState.TO_RETRY_WRITE ||
                    //        processingInfo.itemState == ItemState.TO_SKIP) {
                    //    continue;
                    //}
                }
            } catch (final Exception e) {
                final int txStatus = tm.getStatus();
                if (txStatus == Status.STATUS_ACTIVE || txStatus == Status.STATUS_MARKED_ROLLBACK ||
                        txStatus == Status.STATUS_PREPARED || txStatus == Status.STATUS_PREPARING ||
                        txStatus == Status.STATUS_COMMITTING || txStatus == Status.STATUS_ROLLING_BACK) {
                    tm.rollback();
                    stepMetrics.increment(Metric.MetricType.ROLLBACK_COUNT, 1);
                } else if (txStatus == Status.STATUS_ROLLEDBACK || txStatus == Status.STATUS_NO_TRANSACTION) {
                    //the transaction might have been cancelled by a reaper thread, but has not been disassociated from
                    //the current thread, so call tm.suspend() to safely disassociate it.
                    tm.suspend();
                    stepMetrics.increment(Metric.MetricType.ROLLBACK_COUNT, 1);
                }
                
                for (final ChunkListener l : chunkListeners) {
                    l.onError(e);
                }
                LOGGER.log(Logger.Level.ERROR, processingInfo);
                throw e;
            }
        }
    }

    private void readItem(final ProcessingInfo processingInfo) throws Exception {
        try {
            for (final ItemReadListener l : itemReadListeners) {
                l.beforeRead();
            }
            processingInfo.readPosition++;
            itemRead = itemReader.readItem();
            if (itemRead != null) {  //only count successful read
                stepMetrics.increment(Metric.MetricType.READ_COUNT, 1);
                processingInfo.count++;
            } else {
                processingInfo.chunkState = ChunkState.DEPLETED;
            }
            for (final ItemReadListener l : itemReadListeners) {
                l.afterRead(itemRead);
            }
        } catch (Exception e) {
            for (final ItemReadListener l : itemReadListeners) {
                l.onReadError(e);
            }
            toSkipOrRetry(e, processingInfo);
            if (processingInfo.itemState == ItemState.TO_SKIP) {
                for (final SkipReadListener l : skipReadListeners) {
                    l.onSkipReadItem(e);
                }
                stepMetrics.increment(Metric.MetricType.READ_SKIP_COUNT, 1);
                skipCount++;
                itemRead = null;
            } else if (processingInfo.itemState == ItemState.TO_RETRY) {
                for (final RetryReadListener l : retryReadListeners) {
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
            checkIfEndRetry(processingInfo);
            if (processingInfo.itemState == ItemState.RETRYING_READ) {
                processingInfo.itemState = ItemState.RUNNING;
            }
        }
    }

    private void processItem(final ProcessingInfo processingInfo) throws Exception {
        Object output;
        if (itemProcessor != null) {
            try {
                for (final ItemProcessListener l : itemProcessListeners) {
                    l.beforeProcess(itemRead);
                }
                output = itemProcessor.processItem(itemRead);
                for (final ItemProcessListener l : itemProcessListeners) {
                    l.afterProcess(itemRead, output);
                }
                if (output == null) {
                    stepMetrics.increment(Metric.MetricType.FILTER_COUNT, 1);
                }
            } catch (Exception e) {
                for (final ItemProcessListener l : itemProcessListeners) {
                    l.onProcessError(itemRead, e);
                }
                toSkipOrRetry(e, processingInfo);
                if (processingInfo.itemState == ItemState.TO_SKIP) {
                    for (final SkipProcessListener l : skipProcessListeners) {
                        l.onSkipProcessItem(itemRead, e);
                    }
                    stepMetrics.increment(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
                    skipCount++;
                    output = null;
                } else if (processingInfo.itemState == ItemState.TO_RETRY) {
                    for (final RetryProcessListener l : retryProcessListeners) {
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
        checkIfEndRetry(processingInfo);
        if (processingInfo.itemState == ItemState.RETRYING_PROCESS) {
            processingInfo.itemState = ItemState.RUNNING;
        }
    }

    private void checkIfEndRetry(final ProcessingInfo processingInfo) {
        if (processingInfo.chunkState == ChunkState.RETRYING &&
                processingInfo.itemState != ItemState.TO_RETRY_READ &&
                processingInfo.itemState != ItemState.TO_RETRY_PROCESS &&
                processingInfo.itemState != ItemState.TO_RETRY_WRITE &&
                processingInfo.readPosition == processingInfo.failurePoint) {
            //if failurePoint is null, should fail with NPE
            processingInfo.chunkState = ChunkState.TO_END_RETRY;
        }
    }

    private void beginCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        if (checkpointPolicy.equals("item")) {
            if (timeLimit > 0) {
                final Timer timer = new Timer("chunk-checkpoint-timer", true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        processingInfo.timerExpired = true;
                    }
                }, timeLimit * 1000);
            }
        }
        //if chunk is already RETRYING, do not change it to RUNNING
        if (processingInfo.chunkState == ChunkState.TO_RETRY) {
            processingInfo.chunkState = ChunkState.RETRYING;
        } else if (processingInfo.chunkState != ChunkState.RETRYING) {
            processingInfo.chunkState = ChunkState.RUNNING;
        }
    }

    private boolean isReadyToCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        if (jobContext.getJobExecution().isStopRequested()) {
            processingInfo.chunkState = ChunkState.JOB_STOPPING;
            return true;
        }
        if (processingInfo.chunkState == ChunkState.TO_RETRY) {
            return false;
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
        final int outputSize = outputList.size();
        final boolean nothingToWrite = outputSize == 0 && processingInfo.chunkState == ChunkState.DEPLETED;

        //to back up reader and writer checkpointInfo, and if tx commit fails, restore to previous valid state
        //ChunkState.TO_START_NEW and ChunkState.RUNNING here are used to indicate values not set by application.
        Serializable backupReaderCheckpointInfo = ChunkState.TO_START_NEW;
        Serializable backupWriterCheckpointInfo = ChunkState.TO_START_NEW;
        try {
            if (!nothingToWrite) {
                for (final ItemWriteListener l : itemWriteListeners) {
                    l.beforeWrite(outputList);
                }
                itemWriter.writeItems(outputList);
                stepMetrics.increment(Metric.MetricType.WRITE_COUNT, outputSize);
                for (final ItemWriteListener l : itemWriteListeners) {
                    l.afterWrite(outputList);
                }
            }

            for (final ChunkListener l : chunkListeners) {
                l.afterChunk();
            }

            backupReaderCheckpointInfo = stepOrPartitionExecution.getReaderCheckpointInfo();
            backupWriterCheckpointInfo = stepOrPartitionExecution.getWriterCheckpointInfo();

            stepOrPartitionExecution.setReaderCheckpointInfo(itemReader.checkpointInfo());
            stepOrPartitionExecution.setWriterCheckpointInfo(itemWriter.checkpointInfo());
            batchContext.savePersistentData();
            tm.commit();
            backupReaderCheckpointInfo = backupWriterCheckpointInfo = ChunkState.RUNNING;

            stepMetrics.increment(Metric.MetricType.COMMIT_COUNT, 1);
            processingInfo.checkpointPosition = processingInfo.readPosition;
            outputList.clear();

            if (checkpointAlgorithm != null) {
                checkpointAlgorithm.endCheckpoint();
            }

            if (processingInfo.chunkState == ChunkState.JOB_STOPPING) {
                processingInfo.chunkState = ChunkState.JOB_STOPPED;
                batchContext.setBatchStatus(BatchStatus.STOPPED);
            } else if (processingInfo.chunkState != ChunkState.DEPLETED && processingInfo.chunkState != ChunkState.RETRYING) {
                processingInfo.chunkState = ChunkState.TO_START_NEW;
            }
            if (collector != null && !nothingToWrite) {
                partitionWorker.reportData(collector.collectPartitionData(), stepOrPartitionExecution);
            }
        } catch (final Exception e) {
            if (backupWriterCheckpointInfo != ChunkState.TO_START_NEW && backupWriterCheckpointInfo != ChunkState.RUNNING) {
                //restore reader and writer checkpointInfo to previous valid state
                stepOrPartitionExecution.setReaderCheckpointInfo(backupReaderCheckpointInfo);
                stepOrPartitionExecution.setWriterCheckpointInfo(backupWriterCheckpointInfo);
            }

            for (final ItemWriteListener l : itemWriteListeners) {
                l.onWriteError(outputList, e);
            }
            toSkipOrRetry(e, processingInfo);
            if (processingInfo.itemState == ItemState.TO_SKIP) {
                //if requested to stop the job, do not skip to the next item
                if (processingInfo.chunkState == ChunkState.JOB_STOPPING) {
                    processingInfo.chunkState = ChunkState.JOB_STOPPED;
                    batchContext.setBatchStatus(BatchStatus.STOPPED);
                } else if (processingInfo.chunkState != ChunkState.JOB_STOPPED) {
                    for (final SkipWriteListener l : skipWriteListeners) {
                        l.onSkipWriteItem(outputList, e);
                    }
                    stepMetrics.increment(Metric.MetricType.WRITE_SKIP_COUNT, 1);
                    skipCount++;
                    outputList.clear();

                    //usually the transaction should not be rolled back upon skippable exception, but if the transaction
                    //is marked rollback only by other parties, it's no longer usable and so roll it back.
                    if (tm.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                        tm.rollback();
                    }

                    //during normal processing, upon a skippable exception in writer that is not configured to be
                    //retryable at the same time (i.e., the exception resolved to skip), skip all items in the chunk,
                    //and on to a new chunk
                    if (processingInfo.chunkState == ChunkState.RUNNING) {
                        //processingInfo.itemState = ItemState.RUNNING;
                        processingInfo.chunkState = ChunkState.TO_START_NEW;
                    }
                }
            } else if (processingInfo.itemState == ItemState.TO_RETRY) {
                for (final RetryWriteListener l : retryWriteListeners) {
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

        checkIfEndRetry(processingInfo);
        if (processingInfo.itemState == ItemState.RETRYING_WRITE) {
            processingInfo.itemState = ItemState.RUNNING;
        }
    }

    private void rollbackCheckpoint(final ProcessingInfo processingInfo) throws Exception {
        outputList.clear();
        processingInfo.failurePoint = processingInfo.readPosition;

        //when chunk commit failed, the transaction was already rolled back and its status is STATUS_NO_TRANSACTION (6)
        if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
            tm.rollback();
        }

        stepMetrics.increment(Metric.MetricType.ROLLBACK_COUNT, 1);
        // Close the reader and writer
        try {
            closeItemWriter();
            closeItemReader();
        } catch (Exception e) {
            // An error occurred, safely close the reader and writer
            safeClose();
            throw e;
        }

        tm.begin();
        try {
            // Open the reader and writer
            itemReaderClosed.set(false);
            itemReader.open(stepOrPartitionExecution.getReaderCheckpointInfo());
            processingInfo.readPosition = processingInfo.checkpointPosition;
            itemWriterClosed.set(false);
            itemWriter.open(stepOrPartitionExecution.getWriterCheckpointInfo());
            tm.commit();
        } catch (final Exception e) {
            tm.rollback();
            // An error occurred, safely close the reader and writer
            safeClose();
            throw e;
        }
        processingInfo.chunkState = ChunkState.TO_RETRY;
        processingInfo.itemState = ItemState.RUNNING;
        if (collector != null) {
            partitionWorker.reportData(collector.collectPartitionData(), stepOrPartitionExecution);
        }
    }

    private boolean needSkip(final Exception e) {
        return skippableExceptionClasses != null &&
                ((skipLimit >= 0 && skipCount < skipLimit) || skipLimit < 0) &&
                skippableExceptionClasses.matches(e.getClass());
    }

    private boolean needRetry(final Exception e) {
        return retryableExceptionClasses != null &&
                ((retryLimit >= 0 && retryCount < retryLimit) || retryLimit < 0) &&
                retryableExceptionClasses.matches(e.getClass());
    }

    private void toSkipOrRetry(final Exception e, final ProcessingInfo processingInfo) {
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
    private boolean needRollbackBeforeRetry(final Exception e) {
        //if no-rollback-exceptions not configured, by default need to rollback the current chunk
        //else if the current exception does not match the configured no-rollback-exceptions, need to rollback
        return noRollbackExceptionClasses == null || !noRollbackExceptionClasses.matches(e.getClass());
    }

    private void createChunkRelatedListeners() {
        final Listeners listeners = batchContext.getStep().getListeners();
        if (listeners == null) {
            return;
        }
        String ref;
        Object o;
        for (final RefArtifact l : listeners.getListeners()) {
            ref = l.getRef();
            o = jobContext.createArtifact(ref, null, l.getProperties(), batchContext);
            allChunkRelatedListeners.add(o);
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

    /**
     * Closes the reader and writer swallowing any exceptions
     */
    private void safeClose() {
        try {
            if (itemWriter != null) closeItemWriter();
        } catch (Exception e) {
            LOGGER.trace("Error closing ItemWriter.", e);
        }
        try {
            if (itemReader != null) closeItemReader();
        } catch (Exception e) {
            LOGGER.trace("Error closing ItemReader.", e);
        }
    }

    private void closeItemReader() throws Exception {
        if (itemReaderClosed.compareAndSet(false, true)) {
            itemReader.close();
        }
    }

    private void closeItemWriter() throws Exception {
        if (itemWriterClosed.compareAndSet(false, true)) {
            itemWriter.close();
        }
    }


    private static final class ProcessingInfo {
        /**
         * Number of items completed in during a checkpoint interval
         */
        int count;

        boolean timerExpired;
        ItemState itemState = ItemState.RUNNING;
        ChunkState chunkState = ChunkState.TO_START_NEW;

        /**
         * Used to remember the last checkpoint position in the reader intput data
         */
        int checkpointPosition = -1;

        /**
         * Current position in the reader input data as 0-based int
         */
        int readPosition = -1;

        /**
         * Where the failure occurred that caused the current retry.  The retry should stop after the item at failurePoint
         * has been retried.
         */
        Integer failurePoint;

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

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ProcessingInfo{");
            sb.append("count=").append(count);
            sb.append(", timerExpired=").append(timerExpired);
            sb.append(", itemState=").append(itemState);
            sb.append(", chunkState=").append(chunkState);
            sb.append(", checkpointPosition=").append(checkpointPosition);
            sb.append(", readPosition=").append(readPosition);
            sb.append(", failurePoint=").append(failurePoint);
            sb.append('}');
            return sb.toString();
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
        RETRYING_WRITE  //the current item is being re-written, when successful or result in a skip => normal RUNNING
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
