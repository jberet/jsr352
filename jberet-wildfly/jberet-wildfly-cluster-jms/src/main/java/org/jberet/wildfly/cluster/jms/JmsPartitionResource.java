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

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

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
import org.jberet.wildfly.cluster.jms._private.ClusterJmsLogger;
import org.jberet.wildfly.cluster.jms._private.ClusterJmsMessages;

public final class JmsPartitionResource {
    public static final String CONNECTION_FACTORY = "jms/connectionFactory";
    public static final String PARTITION_QUEUE = "jms/partitionQueue";
    public static final String STOP_REQUEST_TOPIC = "jms/stopRequestTopic";

    public static final String MESSAGE_JOB_EXECUTION_ID_KEY = "jobExecutionId";
    public static final String MESSAGE_STEP_EXECUTION_ID_KEY = "stepExecutionId";
    public static final String MESSAGE_TYPE_KEY = "type";
    public static final String MESSAGE_TYPE_PARTITION = "P";
    public static final String MESSAGE_TYPE_RESULT = "R";

    private final Context namingContext;

    public JmsPartitionResource() {
        try {
            namingContext = new InitialContext();
        } catch (NamingException e) {
            throw ClusterJmsMessages.MESSAGES.failedToNewNamingContext(e);
        }
    }

    public <T> T lookUp(final String name) {
        String s = "java:comp/env/" + name;
        try {
            T result = (T) namingContext.lookup(s);
            ClusterJmsLogger.LOGGER.lookupResource(s, result);
            return result;
        } catch (NamingException e) {
            throw ClusterJmsMessages.MESSAGES.failedToLookup(e, s);
        }
    }

    public Queue getPartitionQueue() {
        return lookUp(PARTITION_QUEUE);
    }

    public Topic getStopRequestTopic() {
        return lookUp(STOP_REQUEST_TOPIC);
    }

    public ConnectionFactory getConnectionFactory() {
        return lookUp(CONNECTION_FACTORY);
    }

    public static String getMessageSelector(final String messageType, final long stepExecutionId) {
        return stepExecutionId > 0 ?
                String.format("%s = '%s' AND %s = %s",
                        MESSAGE_TYPE_KEY, messageType, MESSAGE_STEP_EXECUTION_ID_KEY, stepExecutionId) :
                String.format("%s = '%s'", MESSAGE_TYPE_KEY, messageType);
    }

    public static String getMessageSelector(final long jobExecutionId) {
        return String.format("%s = %s", MESSAGE_JOB_EXECUTION_ID_KEY, jobExecutionId);
    }

    public void close() {
        if (namingContext != null) {
            try {
                namingContext.close();
            } catch (NamingException e) {
                ClusterJmsLogger.LOGGER.problemClosingResource(e);
            }
        }
    }

    public static void closeJmsContext(final JMSContext jmsContext) {
        if (jmsContext != null) {
            try {
                jmsContext.close();
            } catch (Exception e) {
                ClusterJmsLogger.LOGGER.problemClosingResource(e);
            }
        }
    }

    public static AbstractJobOperator getJobOperator() {
        final JobOperator operator = BatchRuntime.getJobOperator();
        AbstractJobOperator jobOperator = null;
        if (operator instanceof DelegatingJobOperator) {
            JobOperator delegate = ((DelegatingJobOperator) operator).getDelegate();
            if (delegate instanceof AbstractJobOperator) {
                jobOperator = (AbstractJobOperator) delegate;
            }
        }
        if (jobOperator == null) {
            throw ClusterJmsMessages.MESSAGES.failedToGetJobOperator();
        }
        return jobOperator;
    }

    public static void runPartition(final PartitionInfo partitionInfo,
                                    final BatchEnvironment batchEnvironment,
                                    final JobRepository jobRepository,
                                    final ArtifactFactory artifactFactory,
                                    final ConnectionFactory connectionFactory,
                                    final Queue partitionQueue,
                                    final Topic stopRequestTopic) {
        final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
        final Step step = partitionInfo.getStep();
        final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();
        final long jobExecutionId = jobExecution.getExecutionId();

        final String stopTopicSelector = JmsPartitionResource.getMessageSelector(jobExecutionId);
        final JMSContext stopRequestTopicContext = connectionFactory.createContext();
        final JMSConsumer stopRequestConsumer = stopRequestTopicContext.createConsumer(stopRequestTopic, stopTopicSelector);
        stopRequestConsumer.setMessageListener(stopMessage -> {
            ClusterJmsLogger.LOGGER.receivedStopRequest(jobExecutionId,
                    step.getId(), partitionExecution.getStepExecutionId(),
                    partitionExecution.getPartitionId());
            jobExecution.stop();
        });

        ClusterJmsLogger.LOGGER.receivedPartitionInfo(partitionInfo);
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
