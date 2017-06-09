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

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.jberet.job.model.Step;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobStopNotificationListener;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.PartitionHandler;
import org.jberet.util.BatchUtil;
import org.jberet.vertx.cluster._private.VertxClusterLogger;
import org.jberet.vertx.cluster._private.VertxClusterMessages;

public class VertxPartitionHandler implements PartitionHandler, JobStopNotificationListener {
    private BlockingQueue<Boolean> completedPartitionThreads;

    private BlockingQueue<Serializable> collectorDataQueue;

    private final Vertx vertx;

    private final EventBus eventBus;

    public VertxPartitionHandler(final StepContextImpl stepContext, final Vertx vertx) {
        this.vertx = vertx;
        this.eventBus = vertx.eventBus();

        Handler<Message<Buffer>> receivingResultHandler = new Handler<Message<Buffer>>() {
            public void handle(Message<Buffer> message) {
                Buffer body = message.body();
                final Serializable partitionCollectorData;
                try {
                    partitionCollectorData = BatchUtil.bytesToSerializableObject(body.getBytes(),
                                            stepContext.getJobContext().getClassLoader());
                } catch (Exception e) {
                    throw VertxClusterMessages.MESSAGES.failedToReceivePartitionCollectorData(e);
                }

                if (partitionCollectorData instanceof PartitionExecutionImpl) {
                    if (completedPartitionThreads != null) {
                        completedPartitionThreads.offer(Boolean.TRUE);
                    }
                    final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) partitionCollectorData;
                    final int partitionId = partitionExecution.getPartitionId();
                    VertxClusterLogger.LOGGER.receivedPartitionResult(
                            stepContext.getJobContext().getExecutionId(), stepContext.getStepExecutionId(),
                            partitionId, partitionExecution.getBatchStatus());

                    //put the partition execution from remote node into its enclosing step execution.
                    //The original partition execution stored in step execution now are obsolete.
                    final List<PartitionExecutionImpl> partitionExecutions =
                            stepContext.getStepExecution().getPartitionExecutions();
                    for (int i = 0; i < partitionExecutions.size(); i++) {
                        if (partitionExecutions.get(i).getPartitionId() == partitionId) {
                            partitionExecutions.remove(i);
                            partitionExecutions.add(partitionExecution);
                        }
                    }
                }

                try {
                    collectorDataQueue.put(partitionCollectorData);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        final long stepExecutionId = stepContext.getStepExecutionId();
        eventBus.consumer(VertxPartitionInfo.getCollectorQueueName(stepExecutionId), receivingResultHandler);
    }

    @Override
    public void setResourceTracker(final BlockingQueue<Boolean> completedPartitionThreads) {
        this.completedPartitionThreads = completedPartitionThreads;
    }

    @Override
    public void setCollectorDataQueue(final BlockingQueue<Serializable> collectorDataQueue) {
        this.collectorDataQueue = collectorDataQueue;
    }

    @Override
    public void submitPartitionTask(final StepContextImpl partitionStepContext) throws Exception {
        final Step step1 = partitionStepContext.getStep();
        final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) partitionStepContext.getStepExecution();
        final JobExecutionImpl jobExecution = partitionStepContext.getJobContext().getJobExecution();

        if (!jobExecution.isStopRequested()) {
            final VertxPartitionInfo partitionInfo = new VertxPartitionInfo(partitionExecution, step1, jobExecution);
            final byte[] bytes = BatchUtil.objectToBytes(partitionInfo);

            //send the partition to another node for execution
            eventBus.send(VertxPartitionInfo.PARTITION_QUEUE, Buffer.buffer(bytes));
        }
    }

    @Override
    public void stopRequested(final long jobExecutionId) {
        eventBus.publish(VertxPartitionInfo.getStopRequestTopicName(jobExecutionId), Boolean.TRUE);
    }
}
