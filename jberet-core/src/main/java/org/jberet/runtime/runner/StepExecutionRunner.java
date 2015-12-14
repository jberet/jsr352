/*
 * Copyright (c) 2012-2015 Red Hat, Inc. and/or its affiliates.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionReducer;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;
import javax.transaction.TransactionManager;

import org.jberet._private.BatchLogger;
import org.jberet.creation.JobScopedContextImpl;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.Listeners;
import org.jberet.job.model.Partition;
import org.jberet.job.model.PartitionPlan;
import org.jberet.job.model.Properties;
import org.jberet.job.model.PropertyResolver;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Step;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.JobTask;
import org.jberet.spi.PropertyKey;
import org.jberet.tx.LocalTransactionManager;

import static org.jberet._private.BatchLogger.LOGGER;
import static org.jberet._private.BatchMessages.MESSAGES;

public final class StepExecutionRunner extends AbstractRunner<StepContextImpl> implements JobTask {
    Step step;
    private final List<StepListener> stepListeners = new ArrayList<StepListener>();
    Map<String, Class<?>> chunkRelatedListeners;

    PartitionMapper mapper;    //programmatic partition config
    PartitionPlan plan;  //static jsl config, mutually exclusive with mapper

    PartitionReducer reducer;
    PartitionAnalyzer analyzer;

    int numOfPartitions;
    int numOfThreads;
    java.util.Properties[] partitionProperties;

    boolean isPartitioned;
    BlockingQueue<Serializable> collectorDataQueue;
    BlockingQueue<Boolean> completedPartitionThreads;

    final TransactionManager tm;
    final StepExecutionImpl stepExecution;

    private boolean analyzerTxEnabled = true;

    public StepExecutionRunner(final StepContextImpl stepContext, final CompositeExecutionRunner enclosingRunner) {
        super(stepContext, enclosingRunner);
        this.step = stepContext.getStep();
        this.stepExecution = (StepExecutionImpl) stepContext.getStepExecution();
        // Determine which TransactionManager to use,
        if (useLocalTx(jobContext, step)) {
            tm = LocalTransactionManager.getInstance();
        } else {
            tm = jobContext.getBatchEnvironment().getTransactionManager();
        }

        if (step.getProperties() != null) {
            analyzerTxEnabled = !Boolean.parseBoolean(step.getProperties().get(PropertyKey.ANALYZER_TX_DISABLED));
        }

        createStepListeners();
        initPartitionConfig();
    }

    @Override
    public void run() {
        final Boolean allowStartIfComplete = batchContext.getAllowStartIfComplete();
        if (allowStartIfComplete != Boolean.FALSE) {
            boolean enterBeforeStep = false;
            try {
                final List<String> executedStepIds = jobContext.getExecutedStepIds();
                if (executedStepIds.contains(step.getId())) {
                    throw MESSAGES.loopbackStep(step.getId(), executedStepIds.toString());
                }

                final int startLimit = step.getStartLimitInt();
                if (startLimit > 0) {
                    final int startCount = jobContext.getJobRepository().countStepStartTimes(stepExecution.getStepName(), jobContext.getInstanceId());
                    if (startCount >= startLimit) {
                        throw MESSAGES.stepReachedStartLimit(step.getId(), startLimit, startCount);
                    }
                }

                batchContext.setBatchStatus(BatchStatus.STARTED);
                jobContext.getJobRepository().addStepExecution(jobContext.getJobExecution(), stepExecution);

                final Chunk chunk = step.getChunk();
                final RefArtifact batchlet = step.getBatchlet();
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

                enterBeforeStep = true;
                for (final StepListener l : stepListeners) {
                    l.beforeStep();
                }
                runBatchletOrChunk(batchlet, chunk);

                //record the fact this step has been executed
                executedStepIds.add(step.getId());
            } catch (final Throwable e) {
                LOGGER.failToRunJob(e, jobContext.getJobName(), step.getId(), step);
                if (e instanceof Exception) {
                    batchContext.setException((Exception) e);
                } else {
                    batchContext.setException(new BatchRuntimeException(e));
                }
                batchContext.setBatchStatus(BatchStatus.FAILED);
            } finally {
                if (enterBeforeStep) {
                    for (final StepListener l : stepListeners) {
                        try {
                            l.afterStep();
                        } catch (final Throwable e) {
                            BatchLogger.LOGGER.failToRunJob(e, jobContext.getJobName(), step.getId(), l);
                            batchContext.setBatchStatus(BatchStatus.FAILED);
                            if (batchContext.getException() != null) {
                                batchContext.setException(new BatchRuntimeException(e));
                            }
                        }
                    }
                    batchContext.savePersistentData();
                }
            }

            jobContext.destroyArtifact(mapper, reducer, analyzer);
            jobContext.destroyArtifact(stepListeners);
            JobScopedContextImpl.ScopedInstance.destroy(batchContext.getScopedBeans());

            switch (batchContext.getBatchStatus()) {
                case COMPLETED:
                    break;
                case STARTED:
                    batchContext.setBatchStatus(BatchStatus.COMPLETED);
                    break;
                case FAILED:
                    for (final AbstractContext e : batchContext.getOuterContexts()) {
                        e.setBatchStatus(BatchStatus.FAILED);
                    }
                    break;
                case STOPPING:
                    batchContext.setBatchStatus(BatchStatus.STOPPED);
                    break;
            }
        }

        batchContext.getJobContext().getJobRepository().updateStepExecution(stepExecution);
        batchContext.setTransientUserData(null);

        switch (batchContext.getBatchStatus()) {
            case COMPLETED:
                final String next1 = resolveTransitionElements(step.getTransitionElements(), step.getAttributeNext(), false);
                enclosingRunner.runJobElement(next1, stepExecution);
                break;
            case FAILED:
                //transition elements can direct to the next job element even after the current step failed
                final String next2 = resolveTransitionElements(step.getTransitionElements(), null, false);
                if (next2 != null) {
                    //check for possible loopback step
                    if (!jobContext.getExecutedStepIds().contains(next2)) {
                        for (final AbstractContext e : batchContext.getOuterContexts()) {
                            e.setBatchStatus(BatchStatus.STARTED);
                        }
                        enclosingRunner.runJobElement(next2, stepExecution);
                    }
                }
                break;
        }
    }

    private void runBatchletOrChunk(final RefArtifact batchlet, final Chunk chunk) throws Exception {
        if (isPartitioned) {
            beginPartition();
        } else if (chunk != null) {
            final ChunkRunner chunkRunner = new ChunkRunner(batchContext, enclosingRunner, this, chunk);
            chunkRunner.run();
        } else {
            final BatchletRunner batchletRunner = new BatchletRunner(batchContext, enclosingRunner, this, batchlet);
            batchletRunner.run();
        }
    }

    private void beginPartition() throws Exception {
        if (reducer != null) {
            reducer.beginPartitionedStep();
        }
        final boolean isRestart = jobContext.isRestart();
        boolean isOverride = false;
        if (mapper != null) {
            final javax.batch.api.partition.PartitionPlan partitionPlan = mapper.mapPartitions();
            isOverride = partitionPlan.getPartitionsOverride();
            numOfPartitions = partitionPlan.getPartitions();
            numOfThreads = partitionPlan.getThreads();
            numOfThreads = (numOfThreads == 0) ? numOfPartitions : numOfThreads;
            partitionProperties = partitionPlan.getPartitionProperties();
        } else {
            numOfPartitions = plan.getPartitionsInt();
            numOfThreads = plan.getThreadsInt();
            final List<Properties> propertiesList = plan.getPropertiesList();
            partitionProperties = new java.util.Properties[propertiesList.size()];
            for (final Properties props : propertiesList) {
                final int idx = props.getPartition() == null ? 0 : Integer.parseInt(props.getPartition());
                partitionProperties[idx] = org.jberet.job.model.Properties.toJavaUtilProperties(props);
            }
        }
        final boolean isRestartNotOverride = isRestart && !isOverride;
        List<PartitionExecutionImpl> abortedPartitionExecutionsFromPrevious = null;
        if (isRestartNotOverride) {
            //need to carry over partition execution data from previous run of the same step.
            //for crashed original step execution, some partitions might not have chance to run during the original
            // job execution, and so should be added
            final StepExecutionImpl originalStepExecution = batchContext.getOriginalStepExecution();
            final BatchStatus oldStatus = originalStepExecution.getBatchStatus();
            final long oldStepExecutionId = originalStepExecution.getStepExecutionId();
            if (oldStatus == BatchStatus.FAILED || oldStatus == BatchStatus.STOPPED || oldStatus == BatchStatus.COMPLETED) {
                abortedPartitionExecutionsFromPrevious =
                        jobContext.getJobRepository().getPartitionExecutions(oldStepExecutionId,
                                originalStepExecution, true, jobContext.getClassLoader());
            } else {  //the original step execution terminated abruptly
                final List<PartitionExecutionImpl> partitionExecutionsFromPrevious =
                        jobContext.getJobRepository().getPartitionExecutions(oldStepExecutionId,
                                originalStepExecution, false, jobContext.getClassLoader());
                abortedPartitionExecutionsFromPrevious = new ArrayList<PartitionExecutionImpl>();
                if (numOfPartitions == partitionExecutionsFromPrevious.size()) {
                    for (final PartitionExecutionImpl e : partitionExecutionsFromPrevious) {
                        if (e.getBatchStatus() != BatchStatus.COMPLETED) {
                            abortedPartitionExecutionsFromPrevious.add(e);
                        }
                    }
                } else {
                    final int[] coveredPartitions = new int[numOfPartitions];
                    Arrays.fill(coveredPartitions, 0);
                    for (final PartitionExecutionImpl e : partitionExecutionsFromPrevious) {
                        coveredPartitions[e.getPartitionId()] = 1;
                        if (e.getBatchStatus() != BatchStatus.COMPLETED) {
                            abortedPartitionExecutionsFromPrevious.add(e);
                        }
                    }
                    for (int i = 0; i < coveredPartitions.length; i++) {
                        if (coveredPartitions[i] == 0) {
                            abortedPartitionExecutionsFromPrevious.add(new PartitionExecutionImpl(i, oldStepExecutionId,
                                    originalStepExecution.getStepName(), BatchStatus.FAILED, BatchStatus.FAILED.name(), null, null, null));
                        }
                    }
                }
            }
            numOfPartitions = abortedPartitionExecutionsFromPrevious.size();
        }
        if (numOfPartitions > numOfThreads) {
            completedPartitionThreads = new ArrayBlockingQueue<Boolean>(numOfPartitions);
        }
        collectorDataQueue = new LinkedBlockingQueue<Serializable>();

        for (int i = 0; i < numOfPartitions; i++) {
            final PartitionExecutionImpl partitionExecution = isRestartNotOverride ? abortedPartitionExecutionsFromPrevious.get(i) : null;
            final int partitionIndex = isRestartNotOverride ? partitionExecution.getPartitionId() : i;

            final AbstractRunner<StepContextImpl> runner1;
            final StepContextImpl stepContext1 = batchContext.clone();
            final Step step1 = stepContext1.getStep();
            final PartitionExecutionImpl partitionExecution1 = (PartitionExecutionImpl) stepContext1.getStepExecution();

            final PropertyResolver resolver = new PropertyResolver();
            if (partitionIndex >= 0 && partitionIndex < partitionProperties.length) {
                resolver.setPartitionPlanProperties(partitionProperties[partitionIndex]);

                //associate this partition represented by this StepExecutionImpl with this partition properties index.  If this
                //partition fails or is stopped, the restart process can select this partition properties.
                partitionExecution1.setPartitionId(partitionIndex);
            } else {
                //some partitioned steps may not have any partition properties
                partitionExecution1.setPartitionId(i);
            }
            resolver.setResolvePartitionPlanProperties(true);
            resolver.resolve(step1);

            if (isRestartNotOverride) {
                stepContext1.setPersistentUserData(partitionExecution.getPersistentUserData());
                stepContext1.getStepExecution().setReaderCheckpointInfo(partitionExecution.getReaderCheckpointInfo());
                stepContext1.getStepExecution().setWriterCheckpointInfo(partitionExecution.getWriterCheckpointInfo());
            }

            if (isRestart && isOverride && reducer != null) {
                reducer.rollbackPartitionedStep();
            }
            final Chunk ch = step1.getChunk();
            if (ch == null) {
                runner1 = new BatchletRunner(stepContext1, enclosingRunner, this, step1.getBatchlet());
            } else {
                runner1 = new ChunkRunner(stepContext1, enclosingRunner, this, ch);
            }
            if (i >= numOfThreads) {
                completedPartitionThreads.take();
            }
            jobContext.getJobRepository().addPartitionExecution(stepExecution, partitionExecution1);
            jobContext.getBatchEnvironment().submitTask(runner1);
        }

        BatchStatus consolidatedBatchStatus = BatchStatus.STARTED;
        final List<PartitionExecutionImpl> fromAllPartitions = new ArrayList<PartitionExecutionImpl>();

        if (analyzer != null && analyzerTxEnabled) {
            tm.begin();
        }
        try {
            while (fromAllPartitions.size() < numOfPartitions) {
                final Serializable data = collectorDataQueue.take();
                if (data instanceof PartitionExecutionImpl) {
                    final PartitionExecutionImpl s = (PartitionExecutionImpl) data;

                    if (step.getChunk() != null) {
                        stepExecution.getStepMetrics().addStepMetrics(s.getStepMetrics());
                    }
                    //save status and data for the terminated partition
                    jobContext.getJobRepository().savePersistentData(jobContext.getJobExecution(), s);

                    fromAllPartitions.add(s);
                    final BatchStatus bs = s.getBatchStatus();

                    if (bs == BatchStatus.FAILED || bs == BatchStatus.STOPPED) {
                        if (consolidatedBatchStatus != BatchStatus.FAILED) {
                            consolidatedBatchStatus = bs;
                        }
                        if (batchContext.getException() == null && s.getException() != null) {
                            batchContext.setException(s.getException());
                        }
                    }

                    if (analyzer != null) {
                        analyzer.analyzeStatus(bs, s.getExitStatus());
                    }
                } else if (analyzer != null) {
                    analyzer.analyzeCollectorData(data);
                }
            }

            if (analyzer != null && analyzerTxEnabled &&
                    (consolidatedBatchStatus == BatchStatus.FAILED || consolidatedBatchStatus == BatchStatus.STOPPED)) {
                tm.rollback();
            } else {
                if (reducer != null) {
                    reducer.beforePartitionedStepCompletion();
                }
                if (analyzer != null && analyzerTxEnabled) {
                    tm.commit();
                }
            }
            if (reducer != null) {
                if (consolidatedBatchStatus == BatchStatus.FAILED || consolidatedBatchStatus == BatchStatus.STOPPED) {
                    reducer.rollbackPartitionedStep();
                    reducer.afterPartitionedStepCompletion(PartitionReducer.PartitionStatus.ROLLBACK);
                } else {
                    reducer.afterPartitionedStepCompletion(PartitionReducer.PartitionStatus.COMMIT);
                }
            }
        } catch (final Exception e) {
            BatchLogger.LOGGER.failToRunJob(e, jobContext.getJobName(), step.getId(), step);
            consolidatedBatchStatus = BatchStatus.FAILED;

            if (analyzer != null && analyzerTxEnabled) {
                try {
                    tm.rollback();
                } catch (final Exception ee) {
                    BatchLogger.LOGGER.tracef(ee, "Exception when rolling back transaction.");
                }
            }
            if (reducer != null) {
                reducer.rollbackPartitionedStep();
                reducer.afterPartitionedStepCompletion(PartitionReducer.PartitionStatus.ROLLBACK);
            }
        }
        batchContext.setBatchStatus(consolidatedBatchStatus);
    }

    private void initPartitionConfig() {
        final Partition partition = step.getPartition();
        if (partition != null) {
            isPartitioned = true;
            final RefArtifact reducerConfig = partition.getReducer();
            if (reducerConfig != null) {
                reducer = jobContext.createArtifact(reducerConfig.getRef(), null, reducerConfig.getProperties(), batchContext);
            }
            final RefArtifact mapperConfig = partition.getMapper();
            if (mapperConfig != null) {
                mapper = jobContext.createArtifact(mapperConfig.getRef(), null, mapperConfig.getProperties(), batchContext);
            }
            final RefArtifact analyzerConfig = partition.getAnalyzer();
            if (analyzerConfig != null) {
                analyzer = jobContext.createArtifact(analyzerConfig.getRef(), null, analyzerConfig.getProperties(), batchContext);
            }
            plan = partition.getPlan();
        }
    }

    private void createStepListeners() {
        final Listeners listeners = step.getListeners();
        if (listeners == null) {
            return;
        }
        String ref;
        for (final RefArtifact listener : listeners.getListeners()) {
            ref = listener.getRef();
            final Class<?> cls = jobContext.getArtifactClass(ref);

            //a class can implement multiple listener interfaces, so need to check it against all listener types
            //even after previous matches
            if (StepListener.class.isAssignableFrom(cls)) {
                final Object o = jobContext.createArtifact(ref, null, listener.getProperties(), batchContext);
                stepListeners.add((StepListener) o);
            }
            if (ChunkListener.class.isAssignableFrom(cls) || ItemReadListener.class.isAssignableFrom(cls) ||
                    ItemWriteListener.class.isAssignableFrom(cls) || ItemProcessListener.class.isAssignableFrom(cls) ||
                    SkipReadListener.class.isAssignableFrom(cls) || SkipWriteListener.class.isAssignableFrom(cls) ||
                    SkipProcessListener.class.isAssignableFrom(cls) || RetryReadListener.class.isAssignableFrom(cls) ||
                    RetryWriteListener.class.isAssignableFrom(cls) || RetryProcessListener.class.isAssignableFrom(cls)
                    ) {
                if (chunkRelatedListeners == null) {
                    chunkRelatedListeners = new HashMap<String, Class<?>>();
                }
                chunkRelatedListeners.put(ref, cls);
            }
        }
    }

    private static boolean useLocalTx(final JobContextImpl jobContext, final Step step) {
        // Jobs parameters passed to the start should always be preferred
        if (jobContext.getJobExecution().getJobParameters() != null) {
            final String value = jobContext.getJobExecution().getJobParameters().getProperty(PropertyKey.LOCAL_TX);
            if (value != null) {
                return "true".equalsIgnoreCase(value);
            }
        }
        // Next any step parameters should take precedence
        if (step.getProperties() != null) {
            final String value = step.getProperties().get(PropertyKey.LOCAL_TX);
            if (value != null) {
                return "true".equalsIgnoreCase(value);
            }
        }
        // Finally job parameters set in the job.xml
        if (jobContext.getJob().getProperties() != null) {
            final String value = jobContext.getJob().getProperties().get(PropertyKey.LOCAL_TX);
            if (value != null) {
                return "true".equalsIgnoreCase(value);
            }
        }
        // Not found use the standard TransactionManager
        return false;
    }
}
