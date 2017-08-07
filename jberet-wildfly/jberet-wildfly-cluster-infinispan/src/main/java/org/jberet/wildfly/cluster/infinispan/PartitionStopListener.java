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

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanLogger;

/**
 * A clustered listener for Infinispan cache.
 */
@Listener(clustered = false)
public class PartitionStopListener {
    private final PartitionInfo partitionInfo;

    public PartitionStopListener(final PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    @CacheEntryModified
    public void entryModified(CacheEntryModifiedEvent event) {
        final Object key = event.getKey();
        final Object value = event.getValue();
        final Cache cache = event.getCache();

        final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
        final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();
        ClusterInfinispanLogger.LOGGER.receivedStopRequest(jobExecution.getExecutionId(),
                partitionExecution.getStepName(),
                partitionExecution.getStepExecutionId(),
                partitionExecution.getPartitionId());
        jobExecution.stop();
    }
}
