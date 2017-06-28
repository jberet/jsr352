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
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.JobRepository;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.jms.JmsPartitionResource;
import org.jberet.wildfly.cluster.jms._private.ClusterJmsMessages;

@WebListener
public class PartitionContextListener implements ServletContextListener {
    private Topic stopRequestTopic;

    private JMSContext partitionQueueContext;
    private Queue partitionQueue;

    private JmsPartitionResource jmsPartitionResource;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final AbstractJobOperator jobOperator = JmsPartitionResource.getJobOperator();
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
                throw ClusterJmsMessages.MESSAGES.failedInJms(e);
            }
            JmsPartitionResource.runPartition(partitionInfo, batchEnvironment, jobRepository, artifactFactory,
                    connectionFactory, partitionQueue, stopRequestTopic);
        });
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        jmsPartitionResource.close();
        JmsPartitionResource.closeJmsContext(partitionQueueContext);
    }
}
