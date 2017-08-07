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

import java.io.Serializable;
import java.util.List;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanLogger;
import org.jboss.logging.Logger;

/**
 * A clustered listener for Infinispan cache.
 * Important: Clustered listener can only be notified for limited events:
 * <ul>
 * <li>CacheEntryRemoved
 * <li>CacheEntryCreated
 * <li>CacheEntryModified
 * <li>CacheEntryExpired
 * </ul>
 */
@Listener(clustered = true)
public class PartitionResultListener {
    private final InfinispanPartitionHandler infinispanPartitionHandler;
    private final StepContextImpl stepContext;

    public PartitionResultListener(final InfinispanPartitionHandler infinispanPartitionHandler,
                                   final StepContextImpl stepContext) {
        this.infinispanPartitionHandler = infinispanPartitionHandler;
        this.stepContext = stepContext;
    }

    @CacheEntryModified
    public void entryModified(CacheEntryModifiedEvent event) {
        final Object key = event.getKey();
        final Object value = event.getValue();

        ClusterInfinispanLogger.LOGGER.logf(Logger.Level.INFO, "## in entryModified key=%s, value=%s", key, value);

        final Serializable partitionCollectorData = (Serializable) value;
        if (partitionCollectorData instanceof PartitionExecutionImpl) {
            if (infinispanPartitionHandler.completedPartitionThreads != null) {
                infinispanPartitionHandler.completedPartitionThreads.offer(Boolean.TRUE);
            }
            final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) partitionCollectorData;
            final int partitionId = partitionExecution.getPartitionId();
            ClusterInfinispanLogger.LOGGER.receivedPartitionResult(
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
            infinispanPartitionHandler.collectorDataQueue.put(partitionCollectorData);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
