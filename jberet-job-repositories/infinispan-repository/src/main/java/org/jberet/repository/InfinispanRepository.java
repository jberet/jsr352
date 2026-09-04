/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.function.SerializableBiFunction;
import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.job.model.Job;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.spi.PropertyKey;

public final class InfinispanRepository extends AbstractRepository {
    private static final String DEFAULT_INFINISPAN_XML = "infinispan.xml";

    private Cache<String, Long> sequenceCache;
    private Cache<Long, JobInstanceImpl> jobInstanceCache;
    private Cache<Long, JobExecutionImpl> jobExecutionCache;
    private Cache<Long, StepExecutionImpl> stepExecutionCache;
    private Cache<String, PartitionExecutionImpl> partitionExecutionCache;

    /**
     * The {@link TransactionManager} shared by {@link #stepExecutionCache} and {@link #jobExecutionCache}, or
     * {@code null} when those caches are not transactional (or are not backed by the same transaction manager).
     * It is used to make the checkpoint update of the step execution and of the enclosing job execution atomic.
     *
     * @see #runAtomically(Runnable)
     */
    private TransactionManager transactionManager;

    private final EmbeddedCacheManager cacheManager;

    public static InfinispanRepository create(final Configuration infinispanConfig) {
        return new InfinispanRepository(infinispanConfig);
    }

    public static InfinispanRepository create(final Properties configProperties) {
        String infinispanXml = configProperties.getProperty(PropertyKey.INFINISPAN_XML);
        if (infinispanXml == null || infinispanXml.isEmpty()) {
            infinispanXml = DEFAULT_INFINISPAN_XML;
        }
        return new InfinispanRepository(infinispanXml);
    }

    public InfinispanRepository(final Configuration infinispanConfig) {
        ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder();
        holder.newConfigurationBuilder("default").read(infinispanConfig);
        cacheManager = new DefaultCacheManager(holder, true);
        initCaches();
    }

    public InfinispanRepository(final String infinispanXml) {
        try {
            cacheManager = new DefaultCacheManager(infinispanXml);
        } catch (final IOException e) {
            throw BatchMessages.MESSAGES.failToCreateCacheManager(e, infinispanXml);
        }
        initCaches();
    }

    @Override
    public void removeJob(final String jobId) {
        super.removeJob(jobId);

        //do not cascade-remove JobInstance or JobExecution, since it will be too expensive in a distributed cache, and
        //also too destructive to remove all these historical records.
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        final JobInstanceImpl jobInstance = new JobInstanceImpl(job, applicationName, job.getId());
        insertJobInstance(jobInstance);
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceIdToRemove) {
        jobInstanceCache.remove(jobInstanceIdToRemove);
    }

    @Override
    public void removeJobExecutions(final JobExecutionSelector jobExecutionSelector) {
        final Collection<Long> allJobExecutionIds = jobExecutionCache.keySet();
        for (final Iterator<Map.Entry<Long, JobExecutionImpl>> it = jobExecutionCache.entrySet().iterator(); it.hasNext(); ) {
            final JobExecutionImpl je = it.next().getValue();
            if (je != null &&
                    (jobExecutionSelector == null || jobExecutionSelector.select(je, allJobExecutionIds))) {
                if (je.getJobParameters() != null) {
                    je.getJobParameters().clear();
                }
                BatchLogger.LOGGER.removing(JobExecution.class.getName(), String.valueOf(je.getExecutionId()));
                it.remove();
            }
        }
    }

    @Override
    public JobInstance getJobInstance(final long jobInstanceId) {
        return jobInstanceCache.get(jobInstanceId);
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        final List<JobInstance> result = new ArrayList<JobInstance>();
        final long largestJobInstanceId = sequenceCache.get(TableColumns.JOB_INSTANCE_ID_SEQ);

        final boolean selectAll = jobName == null || jobName.equals("*");
        for (long i = largestJobInstanceId; i > 0; i--) {
            final JobInstanceImpl e = jobInstanceCache.get(i);
            if (e != null && (selectAll || jobName.equals(e.getJobName()))) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        int count = 0;
        final long largestJobInstanceId = sequenceCache.get(TableColumns.JOB_INSTANCE_ID_SEQ);

        for (long i = largestJobInstanceId; i > 0; i--) {
            final JobInstanceImpl e = jobInstanceCache.get(i);
            if (e != null && e.getJobName().equals(jobName)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        final JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        insertJobExecution(jobExecution);
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        return jobExecutionCache.get(jobExecutionId);
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        List<JobExecution> result;
        if (jobInstance == null) {
            //return all JobExecution
            result = new ArrayList<JobExecution>();
            result.addAll(jobExecutionCache.values());
        } else {
            final long instanceId = jobInstance.getInstanceId();
            result = jobInstanceCache.get(instanceId).getJobExecutions();
            if (result.isEmpty()) {
                result = new ArrayList<JobExecution>();
                for (final JobExecutionImpl e : jobExecutionCache.values()) {
                    if (instanceId == e.getJobInstance().getInstanceId()) {
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        final JobExecutionImpl jobExecution = jobExecutionCache.get(jobExecutionId);
        return jobExecution.getStepExecutions();
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        //super.savePersistentData() serialize persistent data and checkpoint info to avoid further modification
        super.savePersistentData(jobExecution, stepOrPartitionExecution);
        if (stepOrPartitionExecution instanceof StepExecutionImpl) {
            updateStepExecution(stepOrPartitionExecution);
        } else {
            final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) stepOrPartitionExecution;
            partitionExecutionCache.put(
                    concatPartitionExecutionId(partitionExecution.getStepExecutionId(), partitionExecution.getPartitionId()),
                    partitionExecution);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @return 1 if the execution data was saved; 0 if the saved copy of the step or partition execution is
     *         already {@code STOPPING}, in which case nothing is written
     */
    @Override
    public int savePersistentDataIfNotStopping(final JobExecution jobExecution,
                                               final AbstractStepExecution stepOrPartitionExecution) {
        //super.savePersistentData() serialize persistent data and checkpoint info to avoid further modification
        super.savePersistentData(jobExecution, stepOrPartitionExecution);

        if (stepOrPartitionExecution instanceof StepExecutionImpl) {
            //stepExecution is for the main step, and should map to the STEP_EXECUTION cache
            final StepExecutionImpl stepExecution = (StepExecutionImpl) stepOrPartitionExecution;
            final StepExecutionImpl saved = stepExecutionCache.get(stepExecution.getStepExecutionId());
            if (isStopping(saved)) {
                return 0;
            }
            final JobExecutionImpl jobExecutionImpl = resolveJobExecution(jobExecution);
            runAtomically(() -> {
                stepExecutionCache.put(stepExecution.getStepExecutionId(), stepExecution);
                if (jobExecutionImpl != null) {
                    jobExecutionImpl.setLastUpdatedTime(System.currentTimeMillis());
                    jobExecutionCache.put(jobExecutionImpl.getExecutionId(), jobExecutionImpl);
                }
            });
            return 1;
        }

        //stepOrPartitionExecution is a partition execution, and should map to the PARTITION_EXECUTION cache
        final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) stepOrPartitionExecution;
        final String partitionExecutionId =
                concatPartitionExecutionId(partitionExecution.getStepExecutionId(), partitionExecution.getPartitionId());
        if (isStopping(partitionExecutionCache.get(partitionExecutionId))) {
            return 0;
        }
        partitionExecutionCache.put(partitionExecutionId, partitionExecution);
        return 1;
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        stepExecutionCache.put(stepExecution.getStepExecutionId(), (StepExecutionImpl) stepExecution);
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate, final boolean saveJobParameters) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());
        jobExecutionCache.put(jobExecution.getExecutionId(), jobExecution);
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart0,
                                                                 final ClassLoader classLoader) {
        final JobExecutionImpl jobExecutionToRestart = jobExecutionCache.get(jobExecutionToRestart0.getExecutionId());
        for (final StepExecution stepExecution : jobExecutionToRestart.getStepExecutions()) {
            if (stepName.equals(stepExecution.getStepName())) {
                return (StepExecutionImpl) stepExecution;
            }
        }
        StepExecutionImpl result = null;
        // the same-named StepExecution is not found in the jobExecutionToRestart.  It's still possible the same-named
        // StepExecution may exit in JobExecution earlier than jobExecutionToRestart for the same JobInstance.
        final long instanceId = jobExecutionToRestart.getJobInstance().getInstanceId();
        final JobInstanceImpl jobInstanceToRestart = jobInstanceCache.get(instanceId);
        for (final JobExecution jobExecution : jobInstanceToRestart.getJobExecutions()) {
            final JobExecutionImpl jobExecutionImpl = (JobExecutionImpl) jobExecution;
            //skip the JobExecution that has already been checked above
            if (jobExecutionImpl.getExecutionId() != jobExecutionToRestart.getExecutionId()) {
                for (final StepExecution stepExecution : jobExecutionImpl.getStepExecutions()) {
                    if (stepExecution.getStepName().equals(stepName)) {
                        if (result == null || result.getStepExecutionId() < stepExecution.getStepExecutionId()) {
                            result = (StepExecutionImpl) stepExecution;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution, final PartitionExecutionImpl partitionExecution) {
        super.addPartitionExecution(enclosingStepExecution, partitionExecution);
        partitionExecutionCache.put(
                concatPartitionExecutionId(partitionExecution.getStepExecutionId(), partitionExecution.getPartitionId()),
                partitionExecution);
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
                                                               final StepExecutionImpl stepExecution,
                                                               final boolean notCompletedOnly,
                                                               final ClassLoader classLoader) {
        return super.getPartitionExecutions(stepExecutionId, stepExecutionCache.get(stepExecutionId), notCompletedOnly, classLoader);
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        int count = 0;
        final JobInstanceImpl jobInstanceImpl = jobInstanceCache.get(jobInstanceId);
        if (jobInstanceImpl != null) {
            for (final JobExecution jobExecution : jobInstanceImpl.getJobExecutions()) {
                final JobExecutionImpl jobExecutionImpl = jobExecutionCache.get(jobExecution.getExecutionId());
                for (final StepExecution stepExecution : jobExecutionImpl.getStepExecutions()) {
                    if (stepExecution.getStepName().equals(stepName)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        final List<Long> result = new ArrayList<Long>();

        for (final Map.Entry<Long, JobExecutionImpl> e : jobExecutionCache.entrySet()) {
            if (e.getValue().getJobName().equals(jobName)) {
                final BatchStatus s = e.getValue().getBatchStatus();
                if (s == BatchStatus.STARTING || s == BatchStatus.STARTED) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getJobExecutionsByJob(final String jobName) {
        return getJobExecutionsByJob(jobName, null);
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName, Integer limit) {
        CacheStream<Long> stream = jobExecutionCache.values().stream().filter(e -> e.getJobName().equals(jobName))
                .map(JobExecution::getExecutionId)
                .sorted(Comparator.reverseOrder());
        if (limit != null) {
            BatchLogger.LOGGER.jobExecutionRecordsLimited(limit);
            stream = stream.limit(limit);
        }
        return stream.collect(Collectors.toList());
    }

    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        final long jobInstanceId = getNextIdFor(TableColumns.JOB_INSTANCE_ID_SEQ);
        jobInstance.setId(jobInstanceId);
        jobInstanceCache.put(jobInstanceId, jobInstance);
    }

    @Override
    void insertJobExecution(final JobExecutionImpl jobExecution) {
        final long jobExecutionId = getNextIdFor(TableColumns.JOB_EXECUTION_ID_SEQ);
        jobExecution.setId(jobExecutionId);
        jobExecutionCache.put(jobExecutionId, jobExecution);
    }

    @Override
    void insertStepExecution(final StepExecutionImpl stepExecution, final JobExecutionImpl jobExecution) {
        final long stepExecutionId = getNextIdFor(TableColumns.STEP_EXECUTION_ID_SEQ);
        stepExecution.setId(stepExecutionId);
        stepExecutionCache.put(stepExecutionId, stepExecution);
    }

    private void initCaches() {
        sequenceCache = cacheManager.getCache(TableColumns.SEQ, true);
        sequenceCache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD);

        sequenceCache.putIfAbsent(TableColumns.JOB_INSTANCE_ID_SEQ, 0L);
        sequenceCache.putIfAbsent(TableColumns.JOB_EXECUTION_ID_SEQ, 0L);
        sequenceCache.putIfAbsent(TableColumns.STEP_EXECUTION_ID_SEQ, 0L);

        jobInstanceCache = cacheManager.getCache(TableColumns.JOB_INSTANCE, true);
        jobInstanceCache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD);

        jobExecutionCache = cacheManager.getCache(TableColumns.JOB_EXECUTION, true);
        jobExecutionCache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD);

        stepExecutionCache = cacheManager.getCache(TableColumns.STEP_EXECUTION, true);
        stepExecutionCache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD);

        partitionExecutionCache = cacheManager.getCache(TableColumns.PARTITION_EXECUTION, true);
        partitionExecutionCache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD);

        transactionManager = resolveSharedTransactionManager();
    }

    /**
     * Returns the {@link TransactionManager} that both the step execution cache and the job execution cache are
     * enlisted with, or {@code null} if they are non-transactional or use different transaction managers.
     */
    private TransactionManager resolveSharedTransactionManager() {
        try {
            final TransactionManager stepTm = stepExecutionCache.getAdvancedCache().getTransactionManager();
            final TransactionManager jobTm = jobExecutionCache.getAdvancedCache().getTransactionManager();
            return stepTm != null && stepTm == jobTm ? stepTm : null;
        } catch (final Exception e) {
            //a non-transactional cache is not required to expose a transaction manager at all
            BatchLogger.LOGGER.tracef(e, "No shared transaction manager for caches %s and %s%n",
                    TableColumns.STEP_EXECUTION, TableColumns.JOB_EXECUTION);
            return null;
        }
    }

    /**
     * Runs {@code updates} inside a single JTA transaction when the step execution cache and the job execution
     * cache share a {@link TransactionManager}; otherwise runs it directly.
     * If this repository is already running inside a transaction, {@code updates} simply joins it and the caller
     * decides when to commit.
     */
    private void runAtomically(final Runnable updates) {
        if (transactionManager == null) {
            updates.run();
            return;
        }
        boolean startedHere = false;
        try {
            if (transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION) {
                transactionManager.begin();
                startedHere = true;
            }
            updates.run();
            if (startedHere) {
                startedHere = false;
                transactionManager.commit();
            }
        } catch (final Exception e) {
            if (startedHere) {
                try {
                    transactionManager.rollback();
                } catch (final Exception rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
            }
            throw BatchMessages.MESSAGES.failToRunQuery(e, "savePersistentDataIfNotStopping");
        }
    }

    /**
     * Returns the {@link JobExecutionImpl} to update the {@code lastUpdatedTime} on, preferring the live instance
     * passed in by the runtime and falling back to the copy held in the cache.
     */
    private JobExecutionImpl resolveJobExecution(final JobExecution jobExecution) {
        if (jobExecution instanceof JobExecutionImpl) {
            return (JobExecutionImpl) jobExecution;
        }
        return jobExecution == null ? null : jobExecutionCache.get(jobExecution.getExecutionId());
    }

    /**
     * Tells whether a previously saved step or partition execution has already been marked {@code STOPPING}, in
     * which case a checkpoint must not overwrite it. A {@code null} argument means nothing has been saved yet,
     * which is not a stopping condition: the caches are written with {@code put}, so the checkpoint is simply
     * inserted.
     */
    private static boolean isStopping(final AbstractStepExecution savedExecution) {
        return savedExecution != null && savedExecution.getBatchStatus() == BatchStatus.STOPPING;
    }

    /**
     * Returns the cache holding the step executions, so that tests can assert on what was actually written to
     * the cache rather than on the instance they passed in.
     */
    Cache<Long, StepExecutionImpl> getStepExecutionCache() {
        return stepExecutionCache;
    }

    /**
     * Returns the transaction manager that checkpoint writes are enlisted with, or {@code null} when the caches
     * are not transactional. Used by tests to tell the two modes of {@link #runAtomically(Runnable)} apart.
     */
    TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Stops the cache manager created by this repository and releases its resources. Used by tests to tear down
     * an embedded cache manager; a deployed repository lives for as long as its cache manager does.
     */
    void stopCacheManager() {
        cacheManager.stop();
    }

    private long getNextIdFor(final String key) {
        try {
            return sequenceCache.compute(key, (SerializableBiFunction<String, Long, Long>) (k, v) -> v + 1);
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToGetNextId(e, key);
        }
    }

    private static String concatPartitionExecutionId(final long stepExecutionId, final int partitionId) {
        return (new StringBuilder(String.valueOf(stepExecutionId)).append('-').append(String.valueOf(partitionId))).toString();
    }
}
