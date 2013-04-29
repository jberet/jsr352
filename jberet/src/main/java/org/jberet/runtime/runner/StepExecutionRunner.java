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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

    PartitionMapper mapper;    //programmatic partition config
    PartitionPlan plan;  //static jsl config, mutually exclusive with mapper

    PartitionReducer reducer;
    PartitionAnalyzer analyzer;
    Collector collectorConfig;

    int numOfPartitions;
    int numOfThreads;
    java.util.Properties[] partitionProperties;

    boolean isPartitioned;
    BlockingQueue<Serializable> dataQueue;

    UserTransaction ut = TransactionService.getTransaction();

    public StepExecutionRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner) {
        super(stepContext, enclosingRunner);
        this.step = stepContext.getStep();
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
                    runBatchlet(batchlet);
                } else {
                    runChunk(chunk);
                }

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
            enclosingRunner.runJobElement(next, batchContext.getStepExecution());
        }
    }

    private void runBatchlet(Batchlet batchlet) throws Exception {
        BatchletRunner batchletRunner = new BatchletRunner(batchContext, enclosingRunner, this, batchlet);
        if (!isPartitioned) {
            batchletRunner.run();
        } else {
            beginPartition(batchletRunner);
        }
    }

    private void runChunk(Chunk chunk) throws Exception {
        ChunkRunner chunkRunner = new ChunkRunner(batchContext, enclosingRunner, this, chunk);
        if (!isPartitioned) {
            chunkRunner.run();
        } else {
            beginPartition(chunkRunner);
        }
    }

    private void beginPartition(AbstractRunner runner) throws Exception {
        if (reducer != null) {
            reducer.beginPartitionedStep();
        }
        if (mapper != null) {
            javax.batch.api.partition.PartitionPlan partitionPlan = mapper.mapPartitions();
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
        dataQueue = new ArrayBlockingQueue<Serializable>(numOfPartitions * 3);
        for (int i = 0; i < numOfPartitions; i++) {
            AbstractRunner<StepContextImpl> runner1;
            StepContextImpl stepContext1 = batchContext.clone();
            Step step1 = stepContext1.getStep();

            PropertyResolver resolver = new PropertyResolver();
            if (i < partitionProperties.length) {
                resolver.setPartitionPlanProperties(partitionProperties[i]);
            }
            resolver.setResolvePartitionPlanProperties(true);
            resolver.resolve(step1);

            if (runner instanceof BatchletRunner) {
                runner1 = new BatchletRunner(stepContext1, enclosingRunner, this, step1.getBatchlet());
            } else {
                runner1 = new ChunkRunner(stepContext1, enclosingRunner, this, step1.getChunk());
            }
            ConcurrencyService.submit(runner1);
        }

        BatchStatus consolidatedBatchStatus = BatchStatus.STARTED;
        List<StepExecutionImpl> fromAllPartitions = new ArrayList<StepExecutionImpl>();
        ut.begin();
        try {
            while (fromAllPartitions.size() < numOfPartitions) {
                Serializable data = dataQueue.take();
                if (data instanceof StepExecutionImpl) {
                    StepExecutionImpl s = (StepExecutionImpl) data;
                    fromAllPartitions.add(s);
                    BatchStatus bs = s.getBatchStatus();
                    if (bs == BatchStatus.FAILED) {
                        consolidatedBatchStatus= BatchStatus.FAILED;
                    } else if (bs == BatchStatus.STOPPED && consolidatedBatchStatus != BatchStatus.FAILED) {
                        consolidatedBatchStatus = BatchStatus.STOPPED;
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
                reducer = jobContext.createArtifact(reducerConfig.getRef(), reducerConfig.getProperties(), batchContext);
            }
            org.jberet.job.PartitionMapper mapperConfig = partition.getMapper();
            if (mapperConfig != null) {
                mapper = jobContext.createArtifact(mapperConfig.getRef(), mapperConfig.getProperties(), batchContext);
            }
            Analyzer analyzerConfig = partition.getAnalyzer();
            if (analyzerConfig != null) {
                analyzer = jobContext.createArtifact(analyzerConfig.getRef(), analyzerConfig.getProperties(), batchContext);
            }
            collectorConfig = partition.getCollector();
            plan = partition.getPlan();
        }
    }

    private void createStepListeners() {
        Listeners listeners = step.getListeners();
        if (listeners != null) {
            for (Listener listener : listeners.getListener()) {
                //ask the root JobContext to create artifact
                Object o = batchContext.getJobContext().createArtifact(listener.getRef(), listener.getProperties(), batchContext);

                //a class can implement multiple listener interfaces, so need to check it against all listener types
                //even after previous matches
                if (o instanceof StepListener) {
                    stepListeners.add((StepListener) o);
                }
                if (o instanceof ChunkListener) {
                    chunkListeners.add((ChunkListener) o);
                }
                if (o instanceof SkipWriteListener) {
                    skipWriteListeners.add((SkipWriteListener) o);
                }
                if (o instanceof SkipProcessListener) {
                    skipProcessListeners.add((SkipProcessListener) o);
                }
                if (o instanceof SkipReadListener) {
                    skipReadListeners.add((SkipReadListener) o);
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
                if (o instanceof ItemReadListener) {
                    itemReadListeners.add((ItemReadListener) o);
                }
                if (o instanceof ItemWriteListener) {
                    itemWriteListeners.add((ItemWriteListener) o);
                }
                if (o instanceof ItemProcessListener) {
                    itemProcessListeners.add((ItemProcessListener) o);
                }
            }
        }
    }

}
