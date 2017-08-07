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

import org.infinispan.Cache;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.spi.PartitionWorker;
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanLogger;

public class InfinispanPartitionWorker implements PartitionWorker {
    private final Cache<CacheKey, Object> cache;

    private final CacheKey cacheKey;

    private final PartitionStopListener partitionStopListener;

    public InfinispanPartitionWorker(final Cache<CacheKey, Object> cache,
                                     final CacheKey cacheKey,
                                     final PartitionStopListener partitionStopListener) {
        this.cache = cache;
        this.cacheKey = cacheKey;
        this.partitionStopListener = partitionStopListener;
    }

    @Override
    public void reportData(final Serializable data,
                           final AbstractStepExecution partitionExecution) throws Exception {
        final long stepExecutionId = partitionExecution.getStepExecutionId();
        cache.put(cacheKey, data);

        ClusterInfinispanLogger.LOGGER.sendCollectorData(stepExecutionId,
                ((PartitionExecutionImpl) partitionExecution).getPartitionId(), data);
    }

    @Override
    public void partitionDone(final AbstractStepExecution partitionExecution) throws Exception {
        reportData(partitionExecution, partitionExecution);
        cache.removeListener(partitionStopListener);
    }
}
