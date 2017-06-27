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

package org.jberet.wildfly.cluster.jms;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.StreamMessage;
import javax.jms.Topic;

import org.jberet.job.model.Step;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobStopNotificationListener;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.PartitionHandler;
import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.jms._private.ClusterJmsLogger;
import org.jberet.wildfly.cluster.jms._private.ClusterJmsMessages;

import static org.jberet.wildfly.cluster.jms.JmsPartitionResource.MESSAGE_TYPE_KEY;
import static org.jberet.wildfly.cluster.jms.JmsPartitionResource.MESSAGE_TYPE_PARTITION;
import static org.jberet.wildfly.cluster.jms.JmsPartitionResource.MESSAGE_TYPE_RESULT;
import static org.jberet.wildfly.cluster.jms.JmsPartitionResource.getMessageSelector;

public class JmsPartitionHandler implements PartitionHandler, JobStopNotificationListener {
    private BlockingQueue<Boolean> completedPartitionThreads;

    private BlockingQueue<Serializable> collectorDataQueue;

    private final JmsPartitionResource jmsPartitionResource;

    private final JMSContext partitionQueueConsumerContext;
    private final JMSContext partitionQueueProducerContext;
    private final Queue partitionQueue;

    private final ConnectionFactory connectionFactory;

    public JmsPartitionHandler(final StepContextImpl stepContext) {
        this.jmsPartitionResource = new JmsPartitionResource();
        partitionQueue = jmsPartitionResource.getPartitionQueue();
        connectionFactory = jmsPartitionResource.getConnectionFactory();
        partitionQueueConsumerContext = connectionFactory.createContext();
        partitionQueueProducerContext = connectionFactory.createContext();

        final long stepExecutionId = stepContext.getStepExecutionId();
        final String partitionResultSelector = getMessageSelector(
                MESSAGE_TYPE_RESULT, stepExecutionId);
        final JMSConsumer consumer = partitionQueueConsumerContext.createConsumer(partitionQueue, partitionResultSelector);

        consumer.setMessageListener(message -> {
            final Serializable partitionCollectorData;
            try {
                partitionCollectorData = message.getBody(Serializable.class);
            } catch (JMSException e) {
                throw ClusterJmsMessages.MESSAGES.failedInJms(e);
            }
            if (partitionCollectorData instanceof PartitionExecutionImpl) {
                if (completedPartitionThreads != null) {
                    completedPartitionThreads.offer(Boolean.TRUE);
                }
                final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) partitionCollectorData;
                final int partitionId = partitionExecution.getPartitionId();
                ClusterJmsLogger.LOGGER.receivedPartitionResult(
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
        });
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

        final PartitionInfo partitionInfo = new PartitionInfo(partitionExecution, step1, jobExecution);
        final ObjectMessage message = partitionQueueProducerContext.createObjectMessage(partitionInfo);
        message.setStringProperty(MESSAGE_TYPE_KEY, MESSAGE_TYPE_PARTITION);
        partitionQueueProducerContext.createProducer().send(partitionQueue, message);
    }

    @Override
    public void stopRequested(final long jobExecutionId) {
        try (JMSContext stopRequestTopicContext = connectionFactory.createContext()) {
            Topic stopRequestTopic = jmsPartitionResource.getStopRequestTopic();
            final StreamMessage message = stopRequestTopicContext.createStreamMessage();
            try {
                message.setLongProperty(JmsPartitionResource.MESSAGE_JOB_EXECUTION_ID_KEY, jobExecutionId);
                message.writeByte((byte) 1);
                stopRequestTopicContext.createProducer().send(stopRequestTopic, message);
            } catch (JMSException e) {
                throw ClusterJmsMessages.MESSAGES.failedInJms(e);
            }
        }
    }

    @Override
    public void close(final StepContextImpl stepContext) {
        JmsPartitionResource.closeJmsContext(partitionQueueConsumerContext);
        JmsPartitionResource.closeJmsContext(partitionQueueProducerContext);
        jmsPartitionResource.close();
    }
}
