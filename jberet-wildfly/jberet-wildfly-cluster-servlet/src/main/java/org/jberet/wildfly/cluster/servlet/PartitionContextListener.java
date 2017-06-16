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

package org.jberet.wildfly.cluster.servlet;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.Step;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.operations.DelegatingJobOperator;
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
import org.jberet.wildfly.cluster.common.JmsPartitionResource;
import org.jberet.wildfly.cluster.common.JmsPartitionWorker;
import org.jberet.wildfly.cluster.common.org.jberet.wildfly.cluster.common._private.ClusterCommonLogger;
import org.jberet.wildfly.cluster.common.org.jberet.wildfly.cluster.common._private.ClusterCommonMessages;

@WebListener
public class PartitionContextListener implements ServletContextListener {
    private JMSContext stopRequestTopicContext;
    private Topic stopRequestTopic;

    private JMSContext partitionQueueContext;
    private Queue partitionQueue;

    private JmsPartitionResource jmsPartitionResource;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final JobOperator operator = BatchRuntime.getJobOperator();
        AbstractJobOperator jobOperator = null;
        if (operator instanceof DelegatingJobOperator) {
            JobOperator delegate = ((DelegatingJobOperator) operator).getDelegate();
            if (delegate instanceof AbstractJobOperator) {
                jobOperator = (AbstractJobOperator) delegate;
            }
        }
        if (jobOperator == null) {
            throw ClusterCommonMessages.MESSAGES.failedToGetJobOperator(this.toString());
        }

        final BatchEnvironment batchEnvironment = jobOperator.getBatchEnvironment();
        final JobRepository jobRepository = jobOperator.getJobRepository();
        final ArtifactFactoryWrapper artifactFactory = new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory());

        jmsPartitionResource = new JmsPartitionResource();
        partitionQueue = jmsPartitionResource.getPartitionQueue();
        final ConnectionFactory connectionFactory = jmsPartitionResource.getConnectionFactory();
        partitionQueueContext = connectionFactory.createContext();
        stopRequestTopic = jmsPartitionResource.getStopRequestTopic();

        final String messageSelector =
                JmsPartitionResource.getMessageSelector(JmsPartitionResource.MESSAGE_TYPE_PARTITION, 0);

        final JMSConsumer consumer = partitionQueueContext.createConsumer(partitionQueue, messageSelector);
        consumer.setMessageListener(message -> {
            final PartitionInfo partitionInfo;
            try {
                partitionInfo = message.getBody(PartitionInfo.class);
            } catch (JMSException e) {
                throw ClusterCommonMessages.MESSAGES.failedInJms(e);
            }

            final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
            final Step step = partitionInfo.getStep();
            final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();
            final long jobExecutionId = jobExecution.getExecutionId();

            final String stopTopicSelector = JmsPartitionResource.getMessageSelector(jobExecutionId);
            stopRequestTopicContext = connectionFactory.createContext();
            final JMSConsumer stopRequestConsumer = stopRequestTopicContext.createConsumer(stopRequestTopic, stopTopicSelector);
            stopRequestConsumer.setMessageListener(stopMessage -> {
                ClusterCommonLogger.LOGGER.receivedStopRequest(jobExecutionId,
                        step.getId(), partitionExecution.getStepExecutionId(),
                        partitionExecution.getPartitionId());
                jobExecution.stop();
            });

            ClusterCommonLogger.LOGGER.receivedPartitionInfo(partitionInfo);
            final JobContextImpl jobContext = new JobContextImpl(jobExecution, null,
                    artifactFactory, jobRepository, batchEnvironment);

            final JmsPartitionWorker partitionWorker = new JmsPartitionWorker(connectionFactory, partitionQueue);
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
        });
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        jmsPartitionResource.close();
        JmsPartitionResource.closeJmsContext(partitionQueueContext);
        JmsPartitionResource.closeJmsContext(stopRequestTopicContext);
    }
}
