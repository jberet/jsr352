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

package org.jberet.wildfly.cluster.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.jms.JmsPartitionResource;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class PartitionSingletonBean {

    @Resource(name = "jms/connectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(name = "jms/stopRequestTopic")
    private Topic stopRequestTopic;

    @Resource(name = "jms/partitionQueue")
    private Queue partitionQueue;

    private BatchEnvironment batchEnvironment;
    private JobRepository jobRepository;
    private ArtifactFactory artifactFactory;

    @PostConstruct
    private void postConstruct() {
        final AbstractJobOperator jobOperator = JmsPartitionResource.getJobOperator();
        batchEnvironment = jobOperator.getBatchEnvironment();
        jobRepository = jobOperator.getJobRepository();
        artifactFactory = new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory());
    }

    public void runPartition(final PartitionInfo partitionInfo) {
        JmsPartitionResource.runPartition(partitionInfo, batchEnvironment, jobRepository, artifactFactory,
                connectionFactory, partitionQueue, stopRequestTopic);
    }
}
