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
import javax.transaction.UserTransaction;

import org.jberet.job.Analyzer;
import org.jberet.job.Batchlet;
import org.jberet.job.Chunk;
import org.jberet.job.Collector;
import org.jberet.job.Listener;
import org.jberet.job.Listeners;
import org.jberet.job.Partition;
import org.jberet.job.PartitionPlan;
import org.jberet.job.Properties;
import org.jberet.job.Step;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;
import org.jberet.util.ConcurrencyService;
import org.jberet.util.PropertyResolver;
import org.jberet.util.TransactionService;

import static org.jberet.util.BatchLogger.LOGGER;

public final class StepExecutionRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    Step step;
    List<StepListener> stepListeners = new ArrayList<StepListener>();
    Map<String, Class<?>> chunkRelatedListeners;
    
    PartitionMapper mapper;    //programmatic partition config
    PartitionPlan plan;  //static jsl config, mutually exclusive with mapper

    PartitionReducer reducer;
    PartitionAnalyzer analyzer;
    Collector collectorConfig;

    int numOfPartitions;
    int numOfThreads;
    java.util.Properties[] partitionProperties;

    boolean isPartitioned;
    BlockingQueue<Serializable> collectorDataQueue;
    BlockingQueue<Boolean> completedPartitionThreads;

    UserTransaction ut = TransactionService.getTransaction();
    private StepExecutionImpl stepExecution;

    public StepExecutionRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner) {
        super(stepContext, enclosingRunner);
        this.step = stepContext.getStep();
        this.stepExecution = stepContext.getStepExecution();
        createStepListeners();
        initPartitionConfig();
    }

    @Override
    public void run() {
        Boolean allowStartIfComplete = batchContext.getAllowStartIfComplete();
        if (allowStartIfComplete != Boolean.FALSE) {
            try {
                List<Step> executedSteps = batchContext.getJobContext().getExecutedSteps();
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
                    int startCount = stepExecution.getStartCount();
                    if (startCount >= startLimit) {
                        throw LOGGER.stepReachedStartLimit(step.getId(), startLimit, startCount);
                    }
                }

                stepExecution.incrementStartCount();
                batchContext.setBatchStatus(BatchStatus.STARTED);
                batchContext.getJobContext().getJobExecution().addStepExecution(stepExecution);

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

                runBatchletOrChunk(batchlet, chunk);

                //record the fact this step has been executed
                executedSteps.add(step);

                for (StepListener l : stepListeners) {
                    try {
                        l.afterStep();
                    } catch (Throwable e) {
                        BatchLogger.LOGGER.failToRunJob(e, batchContext.getJobContext().getJobName(), step.getId(), l);
                        batchContext.setBatchStatus(BatchStatus.FAILED);
                        return;
                    }
                }
                batchContext.savePersistentData();
            } catch (Throwable e) {
                LOGGER.failToRunJob(e, batchContext.getJobContext().getJobName(), step.getId(), step);
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
                case STOPPING:
                    batchContext.setBatchStatus(BatchStatus.STOPPED);
                    break;
            }
        }

        if (batchContext.getBatchStatus() == BatchStatus.COMPLETED) {
            String next = resolveTransitionElements(step.getTransitionElements(), step.getNext(), false);
            enclosingRunner.runJobElement(next, stepExecution);
        }
    }

    private void runBatchletOrChunk(Batchlet batchlet, Chunk chunk) throws Exception {
        if (isPartitioned) {
            beginPartition();
        } else if (chunk != null) {
            ChunkRunner chunkRunner = new ChunkRunner(batchContext, enclosingRunner, this, chunk);
            chunkRunner.run();
        } else {
            BatchletRunner batchletRunner = new BatchletRunner(batchContext, enclosingRunner, this, batchlet);
            batchletRunner.run();
        }
    }

    private void beginPartition() throws Exception {
        if (reducer != null) {
            reducer.beginPartitionedStep();
        }
        boolean isRestart = batchContext.getJobContext().isRestart();
        boolean isOverride = false;
        if (mapper != null) {
            javax.batch.api.partition.PartitionPlan partitionPlan = mapper.mapPartitions();
            isOverride = partitionPlan.getPartitionsOverride();
            numOfPartitions = partitionPlan.getPartitions();
            numOfThreads = partitionPlan.getThreads();
            partitionProperties = partitionPlan.getPartitionProperties();
        } else {
            numOfPartitions = plan.getPartitions() == null ? 1 : Integer.parseInt(plan.getPartitions());
            numOfThreads = plan.getThreads() == null ? numOfPartitions : Integer.parseInt(plan.getThreads());
            List<Properties> propertiesList = plan.getProperties();
            partitionProperties = new java.util.Properties[propertiesList.size()];
            for (Properties props : propertiesList) {
                int idx = props.getPartition() == null ? 0 : Integer.parseInt(props.getPartition());
                partitionProperties[idx] = BatchUtil.toJavaUtilProperties(props);
            }
        }
        if (isRestart && !isOverride) {
            numOfPartitions = batchContext.getStepExecution().getNumOfPartitions();
        }
        if (numOfPartitions > numOfThreads) {
            completedPartitionThreads = new ArrayBlockingQueue<Boolean>(numOfPartitions);
        }
        collectorDataQueue = new LinkedBlockingQueue<Serializable>();
        List<Integer> indexes = stepExecution.getPartitionPropertiesIndex();

        for (int i = 0; i < numOfPartitions; i++) {
            int partitionIndex = -1;
            if (isRestart && !isOverride) {
                if (indexes != null && i < indexes.size()) {
                    partitionIndex = indexes.get(i);
                }
            } else {
                partitionIndex = i;
            }

            AbstractRunner<StepContextImpl> runner1;
            StepContextImpl stepContext1 = batchContext.clone();
            Step step1 = stepContext1.getStep();

            PropertyResolver resolver = new PropertyResolver();
            if (partitionIndex >= 0 && partitionIndex < partitionProperties.length) {
                resolver.setPartitionPlanProperties(partitionProperties[partitionIndex]);

                //associate this chunk represeted by this StepExecutionImpl with this partition properties index.  If this
                //partition fails or is stopped, the restart process can select this partition properties.
                stepContext1.getStepExecution().addPartitionPropertiesIndex(partitionIndex);
            }
            resolver.setResolvePartitionPlanProperties(true);
            resolver.resolve(step1);

            if (isRestart && !isOverride) {
                List<Serializable> partitionPersistentUserData = stepExecution.getPartitionPersistentUserData();
                if (partitionPersistentUserData != null) {
                    stepContext1.setPersistentUserData(partitionPersistentUserData.get(i));
                }
                List<Serializable> partitionReaderCheckpointInfo = stepExecution.getPartitionReaderCheckpointInfo();
                if (partitionReaderCheckpointInfo != null) {
                    stepContext1.getStepExecution().setReaderCheckpointInfo(partitionReaderCheckpointInfo.get(i));
                }
                List<Serializable> partitionWriterCheckpointInfo = stepExecution.getPartitionWriterCheckpointInfo();
                if (partitionWriterCheckpointInfo != null) {
                    stepContext1.getStepExecution().setWriterCheckpointInfo(partitionWriterCheckpointInfo.get(i));
                }
            }

            if (isRestart && isOverride && reducer != null) {
                reducer.rollbackPartitionedStep();
            }
            Chunk ch = step1.getChunk();
            if (ch == null) {
                runner1 = new BatchletRunner(stepContext1, enclosingRunner, this, step1.getBatchlet());
            } else {
                runner1 = new ChunkRunner(stepContext1, enclosingRunner, this, ch);
            }
            if (i >= numOfThreads) {
                completedPartitionThreads.take();
            }
            ConcurrencyService.submit(runner1);
        }

        BatchStatus consolidatedBatchStatus = BatchStatus.STARTED;
        List<StepExecutionImpl> fromAllPartitions = new ArrayList<StepExecutionImpl>();
        ut.begin();
        try {
            while (fromAllPartitions.size() < numOfPartitions) {
                Serializable data = collectorDataQueue.take();
                if (data instanceof StepExecutionImpl) {
                    StepExecutionImpl s = (StepExecutionImpl) data;
                    fromAllPartitions.add(s);
                    BatchStatus bs = s.getBatchStatus();

                    if (bs == BatchStatus.FAILED || bs == BatchStatus.STOPPED) {
                        List<Integer> idxes = s.getPartitionPropertiesIndex();
                        Integer idx = null;
                        if (idxes != null && idxes.size() > 0) {
                            idx = idxes.get(0);
                        }
                        stepExecution.addPartitionPropertiesIndex(idx);
                        stepExecution.setNumOfPartitions(stepExecution.getNumOfPartitions() + 1);
                        stepExecution.addPartitionPersistentUserData(s.getPersistentUserData());
                        stepExecution.addPartitionReaderCheckpointInfo(s.getReaderCheckpointInfo());
                        stepExecution.addPartitionWriterCheckpointInfo(s.getWriterCheckpointInfo());
                        if(consolidatedBatchStatus != BatchStatus.FAILED) {
                            consolidatedBatchStatus = bs;
                        }
                    }

                    if (analyzer != null) {
                        analyzer.analyzeStatus(bs, s.getExitStatus());
                    }
                } else if (analyzer != null) {
                    analyzer.analyzeCollectorData(data);
                }
            }

            if (consolidatedBatchStatus == BatchStatus.FAILED || consolidatedBatchStatus == BatchStatus.STOPPED) {
                ut.rollback();
            } else {
                if (reducer != null) {
                    reducer.beforePartitionedStepCompletion();
                }
                ut.commit();
            }
            if (reducer != null) {
                if (consolidatedBatchStatus == BatchStatus.FAILED || consolidatedBatchStatus == BatchStatus.STOPPED) {
                    reducer.rollbackPartitionedStep();
                    reducer.afterPartitionedStepCompletion(PartitionReducer.PartitionStatus.ROLLBACK);
                } else {
                    reducer.afterPartitionedStepCompletion(PartitionReducer.PartitionStatus.COMMIT);
                }
            }
        } catch (Exception e) {
            consolidatedBatchStatus = BatchStatus.FAILED;
            if (reducer != null) {
                reducer.rollbackPartitionedStep();
                ut.rollback();
                reducer.afterPartitionedStepCompletion(PartitionReducer.PartitionStatus.ROLLBACK);
            }
        }
        batchContext.setBatchStatus(consolidatedBatchStatus);
    }

    private void initPartitionConfig() {
        Partition partition = step.getPartition();
        if (partition != null) {
            isPartitioned = true;
            JobContextImpl jobContext = batchContext.getJobContext();
            org.jberet.job.PartitionReducer reducerConfig = partition.getReducer();
            if (reducerConfig != null) {
                reducer = jobContext.createArtifact(reducerConfig.getRef(), null, reducerConfig.getProperties(), batchContext);
            }
            org.jberet.job.PartitionMapper mapperConfig = partition.getMapper();
            if (mapperConfig != null) {
                mapper = jobContext.createArtifact(mapperConfig.getRef(), null, mapperConfig.getProperties(), batchContext);
            }
            Analyzer analyzerConfig = partition.getAnalyzer();
            if (analyzerConfig != null) {
                analyzer = jobContext.createArtifact(analyzerConfig.getRef(), null, analyzerConfig.getProperties(), batchContext);
            }
            collectorConfig = partition.getCollector();
            plan = partition.getPlan();
        }
    }

    private void createStepListeners() {
        Listeners listeners = step.getListeners();
        if (listeners != null) {
            String ref;
            for (Listener listener : listeners.getListener()) {
                ref = listener.getRef();
                Class<?> cls = batchContext.getJobContext().getArtifactClass(ref);
                
                //a class can implement multiple listener interfaces, so need to check it against all listener types
                //even after previous matches
                if (StepListener.class.isAssignableFrom(cls)) {
                    if (stepListeners == null) {
                        stepListeners = new ArrayList<StepListener>();
                    }
                    Object o = batchContext.getJobContext().createArtifact(ref, null, listener.getProperties(), batchContext);
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
    }
}
