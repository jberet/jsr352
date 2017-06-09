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

import javax.batch.api.Batchlet;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;

import org.jberet._private.BatchLogger;
import org.jberet.creation.JobScopedContextImpl;
import org.jberet.job.model.RefArtifact;
import org.jberet.runtime.JobStopNotificationListener;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.JobTask;
import org.jberet.spi.PartitionWorker;

import static org.jberet._private.BatchLogger.LOGGER;

public final class BatchletRunner extends AbstractRunner<StepContextImpl> implements JobTask, JobStopNotificationListener {
    private final RefArtifact batchlet;
    private PartitionCollector collector;
    private javax.batch.api.Batchlet batchletObj;
    private PartitionWorker partitionWorker;

    public BatchletRunner(final StepContextImpl stepContext,
                          final CompositeExecutionRunner enclosingRunner,
                          final RefArtifact batchlet,
                          final PartitionWorker partitionWorker) {
        super(stepContext, enclosingRunner);
        this.batchlet = batchlet;
        this.partitionWorker = partitionWorker;
    }

    @Override
    public void stopRequested(final long jobExecutionId) {
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
            if (partitionWorker != null) {
                collectorConfig = batchContext.getStep().getPartition().getCollector();
                if (collectorConfig != null) {
                    collector = jobContext.createArtifact(collectorConfig.getRef(), null, collectorConfig.getProperties(), batchContext);
                }
            }

            batchletObj = (Batchlet) createArtifact(batchlet, batchContext, ScriptBatchlet.class);
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
                partitionWorker.reportData(collector.collectPartitionData(), batchContext.getStepExecution());
            }
        } catch (final Throwable e) {
            batchContext.setException(e instanceof Exception ? (Exception) e : new BatchRuntimeException(e));
            LOGGER.failToRunBatchlet(e, batchlet);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        } finally {
            if (partitionWorker != null) {
                try {
                    JobScopedContextImpl.ScopedInstance.destroy(batchContext.getPartitionScopedBeans());
                    partitionWorker.partitionDone(batchContext.getStepExecution());
                } catch (final Exception e) {
                    BatchLogger.LOGGER.problemFinalizingPartitionExecution(e, batchContext.getStepExecutionId());
                }
            }
            jobContext.destroyArtifact(batchletObj, collector);
        }
    }

}
