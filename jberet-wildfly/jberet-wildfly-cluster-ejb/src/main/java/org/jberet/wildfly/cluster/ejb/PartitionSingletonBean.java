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
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.Topic;

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
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.common.JmsPartitionResource;
import org.jberet.wildfly.cluster.common.JmsPartitionWorker;
import org.jberet.wildfly.cluster.common.org.jberet.wildfly.cluster.common._private.ClusterCommonLogger;
import org.jberet.wildfly.cluster.common.org.jberet.wildfly.cluster.common._private.ClusterCommonMessages;

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
        batchEnvironment = jobOperator.getBatchEnvironment();
        jobRepository = jobOperator.getJobRepository();
        artifactFactory = new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory());
    }

    public void runPartition(final PartitionInfo partitionInfo) {
        final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
        final Step step = partitionInfo.getStep();
        final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();
        final long jobExecutionId = jobExecution.getExecutionId();

        final String stopTopicSelector = JmsPartitionResource.getMessageSelector(jobExecutionId);
        final JMSContext stopRequestTopicContext = connectionFactory.createContext();
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

        final JmsPartitionWorker partitionWorker = new JmsPartitionWorker(connectionFactory, partitionQueue, stopRequestTopicContext);
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
}
