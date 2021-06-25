/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
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
        cacheManager = new DefaultCacheManager(infinispanConfig);
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
        return jobExecutionCache.values().stream().filter(e -> e.getJobName().equals(jobName))
                .map(JobExecution::getExecutionId)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        final long jobInstanceId = getNextIdFor(TableColumns.JOB_INSTANCE_ID_SEQ);
        jobInstance.setId(jobInstanceId);
        jobInstanceCache.put(jobInstanceId, jobInstance);
    }

    @Override
    void insertJobExecution(final JobExecutionImpl jobExecution) {
        final Long jobExecutionId = getNextIdFor(TableColumns.JOB_EXECUTION_ID_SEQ);
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
    }

    private long getNextIdFor(final String key) {
        final long nextId;
        final TransactionManager infinispanTransactionManager = sequenceCache.getAdvancedCache().getTransactionManager();

        try {
            infinispanTransactionManager.begin();
            sequenceCache.getAdvancedCache().lock(key);
            nextId = sequenceCache.get(key) + 1;
            sequenceCache.put(key, nextId);
            infinispanTransactionManager.commit();
            return nextId;
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToGetNextId(e, key);
        }
    }

    private static String concatPartitionExecutionId(final long stepExecutionId, final int partitionId) {
        return (new StringBuilder(String.valueOf(stepExecutionId)).append('-').append(String.valueOf(partitionId))).toString();
    }
}
