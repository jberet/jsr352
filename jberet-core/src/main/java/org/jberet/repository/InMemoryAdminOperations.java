/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.PartitionExecutionImpl;

/**
 * The administrative stale-execution operations implemented over a plain collection of
 * {@link JobExecutionImpl} objects.
 */
final class InMemoryAdminOperations {

    private InMemoryAdminOperations() {
    }

    /**
     * Returns the time a job execution last showed progress: its last-updated time, falling back to
     * its start time and then its create time.
     *
     * @param jobExecution the job execution, not null
     *
     * @return milliseconds since the epoch, or {@code 0} when nothing is recorded
     */
    static long effectiveLastUpdatedTime(final JobExecutionImpl jobExecution) {
        final Date lastUpdated = jobExecution.getLastUpdatedTime();
        if (lastUpdated != null && lastUpdated.getTime() > 0L) {
            return lastUpdated.getTime();
        }
        final Date start = jobExecution.getStartTime();
        if (start != null && start.getTime() > 0L) {
            return start.getTime();
        }
        final Date create = jobExecution.getCreateTime();
        return create == null ? 0L : create.getTime();
    }

    /**
     * Tests the staleness predicate: the execution belongs to {@code jobName}, is in
     * {@code STARTED} or {@code STOPPING}, and last showed progress strictly before the cut-off.
     */
    private static boolean isStale(final JobExecutionImpl jobExecution, final String jobName,
                                   final long cutoff) {
        if (jobExecution == null || !jobName.equals(jobExecution.getJobName())) {
            return false;
        }
        final BatchStatus status = jobExecution.getBatchStatus();
        if (status != BatchStatus.STARTED && status != BatchStatus.STOPPING) {
            return false;
        }
        return effectiveLastUpdatedTime(jobExecution) < cutoff;
    }

    /**
     * @see JobRepository#countStaleExecutions(String, Date)
     */
    static long countStaleExecutions(final Collection<JobExecutionImpl> jobExecutions,
                                     final String jobName, final Date notUpdatedSince) {
        final long cutoff = notUpdatedSince.getTime();
        long count = 0L;
        for (final JobExecutionImpl jobExecution : jobExecutions) {
            if (isStale(jobExecution, jobName, cutoff)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @see JobRepository#findStaleExecutionIds(String, Date, int, int)
     */
    static List<Long> findStaleExecutionIds(final Collection<JobExecutionImpl> jobExecutions,
                                            final String jobName, final Date notUpdatedSince,
                                            final int start, final int count) {
        final long cutoff = notUpdatedSince.getTime();
        final List<Long> matching = new ArrayList<Long>();
        for (final JobExecutionImpl jobExecution : jobExecutions) {
            if (isStale(jobExecution, jobName, cutoff)) {
                matching.add(jobExecution.getExecutionId());
            }
        }
        Collections.sort(matching);
        if (start >= matching.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<Long>(matching.subList(start, Math.min(start + count, matching.size())));
    }

    /**
     * The in-memory counterpart of
     * {@link JobRepository#failJobExecutions(Collection, String, String, Date)}.
     *
     * @param lock       the monitor serialising the write phase, not null
     * @param lookup     resolves an execution id to a job execution, returning null when unknown
     * @param ids        the ids to attempt, already de-duplicated
     * @param jobName    scope filter, or null for no filter
     * @param exitStatus exit status to record on everything changed
     * @param now        timestamp for end time and last-updated time
     * @param afterRead  test hook run between the read and the write phase; may be null
     *
     * @return one outcome per requested id, in iteration order of {@code ids}
     */
    static Map<Long, FailOutcome> failJobExecutions(final Object lock,
                                                    final LongFunction<JobExecutionImpl> lookup,
                                                    final Collection<Long> ids,
                                                    final String jobName,
                                                    final String exitStatus,
                                                    final Date now,
                                                    final Runnable afterRead) {
        final Map<Long, FailOutcome> outcomes = new LinkedHashMap<Long, FailOutcome>();

        // --- phase 1: read and classify, without holding the lock ---
        final Map<Long, BatchStatus> observed = new HashMap<Long, BatchStatus>();
        final List<Long> toFail = new ArrayList<Long>();
        for (final Long id : ids) {
            final JobExecutionImpl jobExecution = id == null ? null : lookup.apply(id);
            if (jobExecution == null) {
                outcomes.put(id, FailOutcome.notFound());
                continue;
            }
            if (jobName != null && !jobName.equals(jobExecution.getJobName())) {
                outcomes.put(id, FailOutcome.notInScope());
                continue;
            }
            final BatchStatus status = jobExecution.getBatchStatus();
            if (status != BatchStatus.STARTED && status != BatchStatus.STOPPING) {
                outcomes.put(id, FailOutcome.invalidStatus(status));
                continue;
            }
            observed.put(id, status);
            toFail.add(id);
        }

        if (afterRead != null) {
            afterRead.run();
        }

        // --- phase 2: write, under the lock, re-checking the guard for each candidate ---
        if (!toFail.isEmpty()) {
            synchronized (lock) {
                for (final Long id : toFail) {
                    final JobExecutionImpl jobExecution = lookup.apply(id);
                    if (jobExecution == null) {
                        outcomes.put(id, FailOutcome.notFound());
                        continue;
                    }
                    final BatchStatus current = jobExecution.getBatchStatus();
                    if (current != BatchStatus.STARTED && current != BatchStatus.STOPPING) {
                        // the guard that the SQL UPDATE's WHERE clause performs
                        outcomes.put(id, FailOutcome.concurrentlyModified(observed.get(id)));
                        continue;
                    }
                    applyFailure(jobExecution, exitStatus, now);
                    outcomes.put(id, FailOutcome.failed());
                }
            }
        }
        return outcomes;
    }

    /**
     * Transitions one job execution and its non-terminal children to {@code FAILED}, children
     * first, leaving terminal children exactly as they are.
     */
    static void applyFailure(final JobExecutionImpl jobExecution, final String exitStatus, final Date now) {
        for (final StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (!(stepExecution instanceof AbstractStepExecution)) {
                continue;
            }
            final AbstractStepExecution step = (AbstractStepExecution) stepExecution;
            if (!isNonTerminal(step.getBatchStatus())) {
                continue;
            }
            //a step's partition list is a plain ArrayList that a still-live runtime thread may be
            //appending to, so iterate a snapshot rather than the list itself
            final List<PartitionExecutionImpl> partitions = step.getPartitionExecutions();
            for (final PartitionExecutionImpl partition
                    : new ArrayList<PartitionExecutionImpl>(partitions == null
                            ? Collections.<PartitionExecutionImpl>emptyList() : partitions)) {
                if (isNonTerminal(partition.getBatchStatus())) {
                    partition.setBatchStatus(BatchStatus.FAILED);
                    partition.setExitStatus(exitStatus);
                }
            }
            step.setBatchStatus(BatchStatus.FAILED);
            step.setExitStatus(exitStatus);
            step.setEndTime(now.getTime());
        }
        jobExecution.setBatchStatus(BatchStatus.FAILED);
        jobExecution.setExitStatus(exitStatus);
        jobExecution.setEndTime(now.getTime());
        jobExecution.setLastUpdatedTime(now.getTime());
    }

    /**
     * @return true for {@code STARTING}, {@code STARTED} and {@code STOPPING}, the statuses the
     *         cascade may overwrite
     */
    static boolean isNonTerminal(final BatchStatus status) {
        return status == BatchStatus.STARTING || status == BatchStatus.STARTED
                || status == BatchStatus.STOPPING;
    }
}
