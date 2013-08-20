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

import java.util.concurrent.TimeUnit;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.model.RefArtifact;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.util.ConcurrencyService;

import static org.jberet.util.BatchLogger.LOGGER;

public final class BatchletRunner extends AbstractRunner<StepContextImpl> implements Runnable {
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

            ConcurrencyService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        jobContext.getJobExecution().awaitStop(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT, TimeUnit.SECONDS);
                        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
                            batchContext.setBatchStatus(BatchStatus.STOPPING);
                            batchletObj.stop();
                        }
                    } catch (Exception e) {
                        LOGGER.failToStopJob(e, jobContext.getJobName(), batchContext.getStepName(), batchletObj);
                    }
                }
            });

            final String exitStatus = batchletObj.process();
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
