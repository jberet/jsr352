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

package org.jberet.wildfly.cluster.infinispan;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.infinispan.Cache;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.operations.DelegatingJobOperator;
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanLogger;
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanMessages;

public final class InfinispanPartitionResource {
    public static final String CACHE_CONTAINER_NAME = "infinispan/container/jberet";
    public static final String CACHE_NAME = "infinispan/cache/web/jberet";

    static final Set<Class<? extends Annotation>> cacheModifiedType =
            Stream.of(CacheEntryModified.class).collect(Collectors.toSet());

    private final Context namingContext;

    public InfinispanPartitionResource() {
        try {
            namingContext = new InitialContext();
        } catch (NamingException e) {
            throw ClusterInfinispanMessages.MESSAGES.failedToNewNamingContext(e);
        }
    }

    public <T> T lookUp(final String name) {
        String s = name.startsWith("java:") ? name : "java:comp/env/" + name;
        try {
            T result = (T) namingContext.lookup(s);
            ClusterInfinispanLogger.LOGGER.lookupResource(s, result);
            return result;
        } catch (NamingException e) {
            throw ClusterInfinispanMessages.MESSAGES.failedToLookup(e, s);
        }
    }

    public Cache<CacheKey, Object> getCache() {
        return lookUp(CACHE_NAME);
    }

    public void close() {
        if (namingContext != null) {
            try {
                namingContext.close();
            } catch (NamingException e) {
                ClusterInfinispanLogger.LOGGER.problemClosingResource(e);
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
            throw ClusterInfinispanMessages.MESSAGES.failedToGetJobOperator();
        }
        return jobOperator;
    }

//    public static void runPartition(final PartitionInfo partitionInfo,
//                                    final BatchEnvironment batchEnvironment,
//                                    final JobRepository jobRepository,
//                                    final ArtifactFactory artifactFactory,
//                                    final ConnectionFactory connectionFactory,
//                                    final Queue partitionQueue,
//                                    final Topic stopRequestTopic) {
//        final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
//        final Step step = partitionInfo.getStep();
//        final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();
//        final long jobExecutionId = jobExecution.getExecutionId();
//
//        final String stopTopicSelector = InfinispanPartitionResource.getMessageSelector(jobExecutionId);
//        final JMSContext stopRequestTopicContext = connectionFactory.createContext();
//        final JMSConsumer stopRequestConsumer = stopRequestTopicContext.createConsumer(stopRequestTopic, stopTopicSelector);
//        stopRequestConsumer.setMessageListener(stopMessage -> {
//            ClusterInfinispanLogger.LOGGER.receivedStopRequest(jobExecutionId,
//                    step.getId(), partitionExecution.getStepExecutionId(),
//                    partitionExecution.getPartitionId());
//            jobExecution.stop();
//        });
//
//        ClusterInfinispanLogger.LOGGER.receivedPartitionInfo(partitionInfo);
//        final JobContextImpl jobContext = new JobContextImpl(jobExecution, null,
//                artifactFactory, jobRepository, batchEnvironment);
//
//        final InfinispanPartitionWorker partitionWorker = new InfinispanPartitionWorker(connectionFactory, partitionQueue, stopRequestTopicContext);
//        final AbstractContext[] outerContext = {jobContext};
//        final StepContextImpl stepContext = new StepContextImpl(step, partitionExecution, outerContext);
//
//        final AbstractRunner<StepContextImpl> runner;
//        final Chunk chunk = step.getChunk();
//        if (chunk == null) {
//            runner = new BatchletRunner(stepContext, null, step.getBatchlet(), partitionWorker);
//        } else {
//            runner = new ChunkRunner(stepContext, null, chunk, null, partitionWorker);
//        }
//        batchEnvironment.submitTask(runner);
//    }
}
