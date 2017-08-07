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
import java.util.concurrent.BlockingQueue;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.jberet.job.model.Step;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobStopNotificationListener;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.PartitionHandler;
import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanLogger;

import static org.jberet.wildfly.cluster.infinispan.InfinispanPartitionResource.cacheModifiedType;

public class InfinispanPartitionHandler implements PartitionHandler, JobStopNotificationListener {
    BlockingQueue<Boolean> completedPartitionThreads;

    BlockingQueue<Serializable> collectorDataQueue;

    private final InfinispanPartitionResource infinispanPartitionResource;

    private final Cache<CacheKey, Object> cache;

    private final PartitionResultListener partitionResultListener;

    public InfinispanPartitionHandler(final StepContextImpl stepContext) {
        this.infinispanPartitionResource = new InfinispanPartitionResource();
        cache = infinispanPartitionResource.getCache();

        final long stepExecutionId = stepContext.getStepExecutionId();
        partitionResultListener = new PartitionResultListener(this, stepContext);
        final PartitionResultFilter filter = new PartitionResultFilter(stepExecutionId);
        cache.addFilteredListener(partitionResultListener, filter, null, cacheModifiedType);
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
    public void submitPartitionTask(final StepContextImpl partitionStepContext,
                                    final int currentIndex,
                                    final int numOfPartitions) throws Exception {
        final Step step1 = partitionStepContext.getStep();
        final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) partitionStepContext.getStepExecution();
        final JobExecutionImpl jobExecution = partitionStepContext.getJobContext().getJobExecution();

        final PartitionInfo partitionInfo = new PartitionInfo(partitionExecution, step1, jobExecution);
        final CacheKey cacheKey = new CacheKey(jobExecution.getExecutionId(), partitionExecution.getStepExecutionId(),
                partitionExecution.getPartitionId());
        cache.put(cacheKey, partitionInfo);

        if (currentIndex == numOfPartitions - 1) {
            final CacheKey[] keys = cache.keySet().toArray(new CacheKey[numOfPartitions]);
            final DistributedExecutorService executorService = new DefaultExecutorService(cache);
            final PartitionCallable partitionCallable = new PartitionCallable();
            executorService.submit(partitionCallable, keys);
            ClusterInfinispanLogger.LOGGER.info("## submitted " + cache.size() + " tasks, " + partitionCallable);
        }
    }

    @Override
    public void stopRequested(final long jobExecutionId) {
        final AdvancedCache<CacheKey, Object> advancedCache = cache.getAdvancedCache();
        final AdvancedCache<CacheKey, Object> cacheLocal = advancedCache.withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD);
        for (CacheKey cacheKey : cacheLocal.keySet()) {
            if (cacheKey.getJobExecutionId() == jobExecutionId) {
                final Object val = cacheLocal.get(cacheKey);
                if (!(val instanceof StopRequest)) {
                    advancedCache.withFlags(Flag.IGNORE_RETURN_VALUES).replace(cacheKey, StopRequest.getInstance());
                }
            }
        }
    }

    @Override
    public void close(final StepContextImpl stepContext) {
        infinispanPartitionResource.close();
        cache.removeListener(partitionResultListener);
    }

}
