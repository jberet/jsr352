/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.FailOutcome;
import org.jberet.repository.JobExecutionSelector;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;

/**
 * A stand-in for {@link JobRepository} that records what the operator asked it and
 * replies with whatever the test set up.
 * <p>
 * The tests below are mostly about <em>what arguments reach the repository</em>, which a
 * recording fake states directly. Every method the administrative operations do not use throws, so
 * a test that accidentally exercises one fails loudly instead of quietly getting a null.
 */
final class FakeJobRepository implements JobRepository {

    final Set<String> knownJobs = new HashSet<String>();
    final List<Long> staleIds = new ArrayList<Long>();
    Long staleCountOverride;
    final Map<Long, FailOutcome> failOutcomes = new LinkedHashMap<Long, FailOutcome>();
    final Map<Long, JobExecutionImpl> executions = new HashMap<Long, JobExecutionImpl>();
    RuntimeException failJobExecutionsError;
    RuntimeException staleQueryError;

    String recordedCountJobName;
    Date recordedCountNotUpdatedSince;
    int countStaleCalls;

    String recordedFindJobName;
    Date recordedFindNotUpdatedSince;
    int recordedFindStart = -1;
    int recordedFindCount = -1;
    int findStaleCalls;

    List<Long> recordedFailIds;
    String recordedFailJobName;
    String recordedFailExitStatus;
    Date recordedFailNow;
    int failCalls;

    /**
     * Registers an execution for the local-liveness screen to find.
     *
     * @param executionId the execution id
     * @param jobName     the job name
     * @param status      the batch status
     * @param live        whether the job instance still holds its unsubstituted job, which is what
     *                    marks an execution as running in this JVM
     */
    void addExecution(final long executionId, final String jobName, final BatchStatus status,
                      final boolean live) {
        final Job job = new JobBuilder(jobName).build();
        final JobInstanceImpl jobInstance = new JobInstanceImpl(live ? job : null, null, jobName);
        jobInstance.setId(executionId);
        executions.put(executionId, new JobExecutionImpl(jobInstance, executionId, new Properties(),
                new Date(), new Date(), null, new Date(), status.name(), null, null));
    }

    @Override
    public boolean jobExists(final String jobName) {
        return knownJobs.contains(jobName);
    }

    @Override
    public JobExecution getJobExecution(final long jobExecutionId) {
        return executions.get(jobExecutionId);
    }

    @Override
    public long countStaleExecutions(final String jobName, final Date notUpdatedSince) {
        countStaleCalls++;
        recordedCountJobName = jobName;
        recordedCountNotUpdatedSince = notUpdatedSince;
        if (staleQueryError != null) {
            throw staleQueryError;
        }
        return staleCountOverride == null ? staleIds.size() : staleCountOverride;
    }

    @Override
    public List<Long> findStaleExecutionIds(final String jobName, final Date notUpdatedSince,
                                            final int start, final int count) {
        findStaleCalls++;
        recordedFindJobName = jobName;
        recordedFindNotUpdatedSince = notUpdatedSince;
        recordedFindStart = start;
        recordedFindCount = count;
        if (staleQueryError != null) {
            throw staleQueryError;
        }
        if (start >= staleIds.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<Long>(staleIds.subList(start, Math.min(start + count, staleIds.size())));
    }

    @Override
    public Map<Long, FailOutcome> failJobExecutions(final Collection<Long> executionIds, final String jobName,
                                                    final String exitStatus, final Date now) {
        failCalls++;
        recordedFailIds = new ArrayList<Long>(executionIds);
        recordedFailJobName = jobName;
        recordedFailExitStatus = exitStatus;
        recordedFailNow = now;
        if (failJobExecutionsError != null) {
            throw failJobExecutionsError;
        }
        final Map<Long, FailOutcome> outcomes = new LinkedHashMap<Long, FailOutcome>();
        for (final Long id : executionIds) {
            final FailOutcome configured = failOutcomes.get(id);
            outcomes.put(id, configured == null ? FailOutcome.failed() : configured);
        }
        return outcomes;
    }

    private static UnsupportedOperationException notUsed() {
        return new UnsupportedOperationException("not used by the administrative operations");
    }

    @Override
    public void addJob(final ApplicationAndJobName applicationAndJobName, final Job job) {
        throw notUsed();
    }

    @Override
    public void removeJob(final String jobId) {
        throw notUsed();
    }

    @Override
    public Job getJob(final ApplicationAndJobName applicationAndJobName) {
        throw notUsed();
    }

    @Override
    public Set<String> getJobNames() {
        return Collections.unmodifiableSet(knownJobs);
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        throw notUsed();
    }

    @Override
    public void removeJobInstance(final long jobInstanceId) {
        throw notUsed();
    }

    @Override
    public JobInstance getJobInstance(final long jobInstanceId) {
        throw notUsed();
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        throw notUsed();
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        throw notUsed();
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        throw notUsed();
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        throw notUsed();
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName) {
        throw notUsed();
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName, final Integer limit) {
        throw notUsed();
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate,
                                   final boolean saveJobParameters) {
        throw notUsed();
    }

    @Override
    public void stopJobExecution(final JobExecutionImpl jobExecution) {
        throw notUsed();
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        throw notUsed();
    }

    @Override
    public void removeJobExecutions(final JobExecutionSelector jobExecutionSelector) {
        throw notUsed();
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        throw notUsed();
    }

    @Override
    public StepExecutionImpl createStepExecution(final String stepName) {
        throw notUsed();
    }

    @Override
    public void addStepExecution(final JobExecutionImpl jobExecution, final StepExecutionImpl stepExecution) {
        throw notUsed();
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        throw notUsed();
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart,
                                                                 final ClassLoader classLoader) {
        throw notUsed();
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        throw notUsed();
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution,
                                      final PartitionExecutionImpl partitionExecution) {
        throw notUsed();
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
                                                               final StepExecutionImpl stepExecution,
                                                               final boolean notCompletedOnly,
                                                               final ClassLoader classLoader) {
        throw notUsed();
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution,
                                   final AbstractStepExecution stepOrPartitionExecution) {
        throw notUsed();
    }

    @Override
    public int savePersistentDataIfNotStopping(final JobExecution jobExecution,
                                               final AbstractStepExecution stepOrPartitionExecution) {
        throw notUsed();
    }
}
