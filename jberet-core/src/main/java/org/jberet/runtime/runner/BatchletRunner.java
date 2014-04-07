/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.BatchStatus;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.RefArtifact;
import org.jberet.runtime.JobStopNotifier;
import org.jberet.runtime.context.StepContextImpl;

import static org.jberet._private.BatchLogger.LOGGER;

public final class BatchletRunner extends AbstractRunner<StepContextImpl> implements Runnable, JobStopNotifier {
    private final RefArtifact batchlet;
    private final StepExecutionRunner stepRunner;
    private PartitionCollector collector;
    private javax.batch.api.Batchlet batchletObj;

    public BatchletRunner(final StepContextImpl stepContext, final CompositeExecutionRunner enclosingRunner, final StepExecutionRunner stepRunner, final RefArtifact batchlet) {
        super(stepContext, enclosingRunner);
        this.stepRunner = stepRunner;
        this.batchlet = batchlet;
    }

    @Override
    public void stopRequested() {
        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
            batchContext.setBatchStatus(BatchStatus.STOPPING);
            if (batchletObj != null) {
                try {
                    batchletObj.stop();
                } catch (final Exception e) {
                    BatchLogger.LOGGER.failToStopJob(e, jobContext.getJobName(), batchContext.getStepName(), batchletObj);
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            final RefArtifact collectorConfig;
            if (stepRunner.collectorDataQueue != null) {
                collectorConfig = batchContext.getStep().getPartition().getCollector();
                if (collectorConfig != null) {
                    collector = jobContext.createArtifact(collectorConfig.getRef(), null, collectorConfig.getProperties(), batchContext);
                }
            }
            batchletObj = jobContext.createArtifact(batchlet.getRef(), null, batchlet.getProperties(), batchContext);
            String exitStatus = null;
            if (jobContext.getJobExecution().isStopRequested()) {
                batchContext.setBatchStatus(BatchStatus.STOPPING);
            } else {
                jobContext.getJobExecution().registerJobStopNotifier(this);
                exitStatus = batchletObj.process();
                jobContext.getJobExecution().unregisterJobStopNotifier(this);
            }

            //set batch status to indicate that either the main step, or a partition has completed successfully.
            //make sure the step has not been set to STOPPED. The same in ChunkRunner.run().
            switch (batchContext.getBatchStatus()) {
                case STARTED:
                    batchContext.setBatchStatus(BatchStatus.COMPLETED);
                    break;
                case STOPPING:
                    batchContext.setBatchStatus(BatchStatus.STOPPED);
                    break;
            }

            batchContext.setExitStatus(exitStatus);
            if (collector != null) {
                stepRunner.collectorDataQueue.put(collector.collectPartitionData());
            }
        } catch (Exception e) {
            //TODO remove this block.  collector is not called for unhandled exceptions.
            try {
                if (collector != null) {
                    stepRunner.collectorDataQueue.put(collector.collectPartitionData());
                }
            } catch (Exception e1) {
                //ignore
            }
            batchContext.setException(e);
            LOGGER.failToRunBatchlet(e, batchlet);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        } finally {
            try {
                if (stepRunner.collectorDataQueue != null) {
                    stepRunner.collectorDataQueue.put(batchContext.getStepExecution());
                }
            } catch (InterruptedException e) {
                //ignore
            }
            if (stepRunner.completedPartitionThreads != null) {
                stepRunner.completedPartitionThreads.offer(Boolean.TRUE);
            }
            jobContext.destroyArtifact(batchletObj, collector);
        }
    }

}
