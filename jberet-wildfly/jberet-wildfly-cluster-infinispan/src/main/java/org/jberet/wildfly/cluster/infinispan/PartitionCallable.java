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
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.Step;
import org.jberet.operations.AbstractJobOperator;
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
import org.jberet.wildfly.cluster.infinispan._private.ClusterInfinispanLogger;

import static org.jberet.wildfly.cluster.infinispan.InfinispanPartitionResource.cacheModifiedType;

public class PartitionCallable implements DistributedCallable<CacheKey, Object, Void>, Serializable {
    private static final long serialVersionUID = -5054226353703356512L;

    private transient Cache<CacheKey, Object> cache;

    private transient Set<CacheKey> inputKeys;

    @Override
    public void setEnvironment(final Cache<CacheKey, Object> cache, final Set<CacheKey> inputKeys) {
        ClusterInfinispanLogger.LOGGER.info("## in setEnvironment, selected keys: " + inputKeys.toString() +
        ", cache size: " + cache.size());
        this.cache = cache;
        this.inputKeys = inputKeys;
    }

    @Override
    public Void call() throws Exception {
        ClusterInfinispanLogger.LOGGER.info("## about to get jobOperator");
        final AbstractJobOperator jobOperator = InfinispanPartitionResource.getJobOperator();
        final BatchEnvironment batchEnvironment = jobOperator.getBatchEnvironment();
        final JobRepository jobRepository = jobOperator.getJobRepository();
        final ArtifactFactory artifactFactory = new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory());

        ClusterInfinispanLogger.LOGGER.info("## got jobOperator: " + jobOperator + "inputKeys: " + inputKeys);

        final AdvancedCache<CacheKey, Object> cacheLocal = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD);
        for (final CacheKey k : inputKeys) {
            final Object val = cacheLocal.get(k);
            if (val instanceof PartitionInfo) {
                final PartitionInfo partitionInfo = (PartitionInfo) val;
                final JobExecutionImpl jobExecution = partitionInfo.getJobExecution();
                final Step step = partitionInfo.getStep();
                final PartitionExecutionImpl partitionExecution = partitionInfo.getPartitionExecution();

                final PartitionStopListener partitionStopListener = new PartitionStopListener(partitionInfo);
                cache.addFilteredListener(partitionStopListener,
                new CacheEventFilter<CacheKey, Object>() {
                    @Override
                    public boolean accept(final CacheKey key, final Object oldValue,
                                          final Metadata oldMetadata, final Object newValue,
                                          final Metadata newMetadata, final EventType eventType) {
                        return newValue instanceof StopRequest &&
                                key.equals(k);
                    }
                }, null, cacheModifiedType);

                ClusterInfinispanLogger.LOGGER.receivedPartitionInfo(partitionInfo);
                final JobContextImpl jobContext = new JobContextImpl(jobExecution, null,
                        artifactFactory, jobRepository, batchEnvironment);

                final InfinispanPartitionWorker partitionWorker = new InfinispanPartitionWorker(cache, k, partitionStopListener);
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

        return null;
    }
}
