/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.Step;
import org.jberet.operations.JobOperatorImpl;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.runtime.runner.AbstractRunner;
import org.jberet.runtime.runner.BatchletRunner;
import org.jberet.runtime.runner.ChunkRunner;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.PartitionInfo;
import org.jberet.util.BatchUtil;
import org.jberet.vertx.cluster._private.VertxClusterLogger;
import org.jberet.vertx.cluster._private.VertxClusterMessages;

public class ChunkPartitionVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        final JobOperatorImpl jobOperator = new JobOperatorImpl();
        final BatchEnvironment batchEnvironment = jobOperator.getBatchEnvironment();
        final JobRepository jobRepository = jobOperator.getJobRepository();
        final ArtifactFactoryWrapper artifactFactory = new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory());

        final EventBus eventBus = vertx.eventBus();
        final Handler<Message<Buffer>> receivingPartitionHandler = new Handler<Message<Buffer>>() {
            public void handle(Message<Buffer> message) {
                Buffer body = message.body();
                final byte[] bytes = body.getBytes();
                final PartitionInfo partitionInfo;
                try {
                    partitionInfo = (PartitionInfo) BatchUtil.bytesToSerializableObject(
                            bytes, batchEnvironment.getClassLoader());
                } catch (Exception e) {
                    throw VertxClusterMessages.MESSAGES.failedToReceivePartitionInfo(e);
                }

                final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
                final Step step = partitionInfo.getStep();
                final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();
                final String stopRequestTopicName =
                        PartitionInfo.getStopRequestTopicName(jobExecution.getExecutionId());
                eventBus.consumer(stopRequestTopicName, new Handler<Message<Boolean>>() {
                            @Override
                            public void handle(final Message<Boolean> stopMessage) {
                                VertxClusterLogger.LOGGER.receivedStopRequest(jobExecution.getExecutionId(),
                                        step.getId(), partitionExecution.getStepExecutionId(),
                                        partitionExecution.getPartitionId());
                                jobExecution.stop();
                            }
                        });

                VertxClusterLogger.LOGGER.receivedPartitionInfo(partitionInfo);
                final JobContextImpl jobContext = new JobContextImpl(jobExecution, null,
                        artifactFactory, jobRepository, batchEnvironment);

                final VertxPartitionWorker partitionWorker = new VertxPartitionWorker(eventBus);
                final AbstractContext[] outerContext = {jobContext};
                final StepContextImpl stepContext = new StepContextImpl(step, partitionExecution, outerContext);

                final AbstractRunner<StepContextImpl> runner;
                final Chunk chunk = step.getChunk();
                if (chunk == null) {
                    runner = new BatchletRunner(stepContext, null, step.getBatchlet(), partitionWorker);
                } else {
                    runner = new ChunkRunner(stepContext, null, chunk, null, partitionWorker);
                }
                batchEnvironment.submitTask(runner);
            }
        };
        eventBus.consumer(PartitionInfo.PARTITION_QUEUE, receivingPartitionHandler);
    }
}
