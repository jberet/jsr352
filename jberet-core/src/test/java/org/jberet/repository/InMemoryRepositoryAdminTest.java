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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.batch.runtime.BatchStatus;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the in-memory implementations of the administrative stale-execution operations.
 * <p>
 * These live in {@link InMemoryRepository} rather than in {@code AbstractPersistentRepository},
 * because that class is a cache in front of a real store: {@code MongoRepository} extends it and
 * would otherwise inherit an implementation that reads and writes only this JVM's cache.
 */
public class InMemoryRepositoryAdminTest {

    private static final String JOB = "inMemoryAdminOpsJob";
    private static final String OTHER_JOB = "inMemoryAdminOpsOtherJob";
    private static final long MINUTE = 60_000L;

    private InMemoryRepository repository;
    private Job job;
    private Job otherJob;

    @BeforeEach
    public void setUp() {
        repository = InMemoryRepository.create();
        job = new JobBuilder(JOB).build();
        otherJob = new JobBuilder(OTHER_JOB).build();
    }

    /**
     * Creates a job execution in the repository and forces its batch status and last-updated time,
     * standing in for an execution whose JVM died {@code minutesAgo} minutes ago.
     */
    private JobExecutionImpl execution(final Job ofJob, final BatchStatus status, final long minutesAgo) {
        final JobInstanceImpl jobInstance = repository.createJobInstance(ofJob, null, getClass().getClassLoader());
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, null);
        jobExecution.setBatchStatus(status);
        jobExecution.setLastUpdatedTime(System.currentTimeMillis() - minutesAgo * MINUTE);
        return jobExecution;
    }

    private StepExecutionImpl step(final JobExecutionImpl jobExecution, final String name,
                                   final BatchStatus status, final String exitStatus) {
        final StepExecutionImpl stepExecution = repository.createStepExecution(name);
        repository.addStepExecution(jobExecution, stepExecution);
        stepExecution.setBatchStatus(status);
        stepExecution.setExitStatus(exitStatus);
        return stepExecution;
    }

    private PartitionExecutionImpl partition(final StepExecutionImpl stepExecution, final int partitionId,
                                             final BatchStatus status, final String exitStatus) {
        final PartitionExecutionImpl partitionExecution = new PartitionExecutionImpl(partitionId,
                stepExecution.getStepExecutionId(), stepExecution.getStepName(), status, exitStatus,
                null, null, null);
        repository.addPartitionExecution(stepExecution, partitionExecution);
        return partitionExecution;
    }

    private static Date minutesAgo(final long minutes) {
        return new Date(System.currentTimeMillis() - minutes * MINUTE);
    }

    // ---------------------------------------------------------------------------------------
    // staleness detection
    // ---------------------------------------------------------------------------------------

    @Test
    public void onlyStartedAndStoppingExecutionsAreStale() {
        execution(job, BatchStatus.STARTED, 60);
        execution(job, BatchStatus.STOPPING, 60);
        execution(job, BatchStatus.STARTING, 60);
        execution(job, BatchStatus.COMPLETED, 60);
        execution(job, BatchStatus.FAILED, 60);
        execution(job, BatchStatus.STOPPED, 60);
        execution(job, BatchStatus.ABANDONED, 60);

        assertEquals(2L, repository.countStaleExecutions(JOB, minutesAgo(30)));
        assertEquals(2, repository.findStaleExecutionIds(JOB, minutesAgo(30), 0, 100).size());
    }

    @Test
    public void executionsOfOtherJobsAreNotStale() {
        execution(job, BatchStatus.STARTED, 60);
        execution(otherJob, BatchStatus.STARTED, 60);

        assertEquals(1L, repository.countStaleExecutions(JOB, minutesAgo(30)));
        assertEquals(1L, repository.countStaleExecutions(OTHER_JOB, minutesAgo(30)));
        assertEquals(0L, repository.countStaleExecutions("notAJob", minutesAgo(30)));
    }

    /**
     * The predicate is strictly less than the cut-off, so an execution updated exactly on the
     * boundary is not yet stale. Without this, "not updated in 30 minutes" would quietly mean
     * "not updated in 30 minutes or updated exactly 30 minutes ago".
     */
    @Test
    public void stalenessBoundaryIsExclusive() {
        final JobExecutionImpl jobExecution = execution(job, BatchStatus.STARTED, 0);
        final long cutoff = System.currentTimeMillis() - 30 * MINUTE;

        jobExecution.setLastUpdatedTime(cutoff);
        assertEquals(0L, repository.countStaleExecutions(JOB, new Date(cutoff)));

        jobExecution.setLastUpdatedTime(cutoff - 1);
        assertEquals(1L, repository.countStaleExecutions(JOB, new Date(cutoff)));
    }

    /**
     * Builds an execution the way a repository does when it reads one back from a persistence
     * store, which is the only way a null last-updated or start time arises: a row inserted by
     * {@code insert-job-execution} carries neither until the first progress update runs.
     */
    private static JobExecutionImpl loadedExecution(final String jobName, final Date createTime,
                                                    final Date startTime, final Date lastUpdatedTime) {
        final JobInstanceImpl jobInstance = new JobInstanceImpl(null, null, jobName);
        return new JobExecutionImpl(jobInstance, 1L, null, createTime, startTime, null, lastUpdatedTime,
                BatchStatus.STARTED.name(), null, null);
    }

    /** An execution that died before its first progress update falls back to its start time. */
    @Test
    public void fallsBackToStartTimeWhenLastUpdatedIsUnset() {
        final JobExecutionImpl jobExecution =
                loadedExecution(JOB, minutesAgo(120), minutesAgo(90), null);
        final List<JobExecutionImpl> all = Collections.singletonList(jobExecution);

        assertEquals(minutesAgo(90).getTime(),
                InMemoryAdminOperations.effectiveLastUpdatedTime(jobExecution), 2000L);
        assertEquals(1L, InMemoryAdminOperations.countStaleExecutions(all, JOB, minutesAgo(60)));
        assertEquals(0L, InMemoryAdminOperations.countStaleExecutions(all, JOB, minutesAgo(120)));
    }

    /** With neither a last-updated nor a start time, the create time decides. */
    @Test
    public void fallsBackToCreateTimeWhenNeitherLastUpdatedNorStartIsSet() {
        final JobExecutionImpl jobExecution = loadedExecution(JOB, minutesAgo(120), null, null);
        final List<JobExecutionImpl> all = Collections.singletonList(jobExecution);

        assertEquals(minutesAgo(120).getTime(),
                InMemoryAdminOperations.effectiveLastUpdatedTime(jobExecution), 2000L);
        assertEquals(1L, InMemoryAdminOperations.countStaleExecutions(all, JOB, minutesAgo(60)));
        assertEquals(0L, InMemoryAdminOperations.countStaleExecutions(all, JOB, minutesAgo(180)));
    }

    /** A last-updated time, when present, wins over the start and create times. */
    @Test
    public void lastUpdatedTimeWinsWhenPresent() {
        final JobExecutionImpl jobExecution =
                loadedExecution(JOB, minutesAgo(120), minutesAgo(110), minutesAgo(5));
        final List<JobExecutionImpl> all = Collections.singletonList(jobExecution);

        assertEquals(0L, InMemoryAdminOperations.countStaleExecutions(all, JOB, minutesAgo(60)),
                "recent progress means not stale, however long ago it started");
        assertEquals(1L, InMemoryAdminOperations.countStaleExecutions(all, JOB, minutesAgo(1)));
    }

    @Test
    public void staleIdsAreOrderedAscendingAndPaged() {
        final List<Long> all = new ArrayList<Long>();
        for (int i = 0; i < 25; i++) {
            all.add(execution(job, BatchStatus.STARTED, 60).getExecutionId());
        }
        Collections.sort(all);

        assertEquals(25L, repository.countStaleExecutions(JOB, minutesAgo(30)));

        final List<Long> walked = new ArrayList<Long>();
        for (int start = 0; start < 30; start += 10) {
            final List<Long> page = repository.findStaleExecutionIds(JOB, minutesAgo(30), start, 10);
            walked.addAll(page);
        }
        assertEquals(all, walked);
        assertTrue(repository.findStaleExecutionIds(JOB, minutesAgo(30), 25, 10).isEmpty());
        assertTrue(repository.findStaleExecutionIds(JOB, minutesAgo(30), 1000, 10).isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // failJobExecutions
    // ---------------------------------------------------------------------------------------

    @Test
    public void failTransitionsTheJobExecution() {
        final JobExecutionImpl jobExecution = execution(job, BatchStatus.STARTED, 60);
        final Date now = new Date();

        final Map<Long, FailOutcome> outcomes = repository.failJobExecutions(
                Collections.singletonList(jobExecution.getExecutionId()), null, "FAILED_BY_ADMIN", now);

        assertEquals(FailOutcome.Kind.FAILED, outcomes.get(jobExecution.getExecutionId()).kind());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals("FAILED_BY_ADMIN", jobExecution.getExitStatus());
        assertEquals(now, jobExecution.getEndTime());
        assertEquals(now, jobExecution.getLastUpdatedTime());
    }

    /** The cascade reaches running steps and partitions and leaves terminal ones alone. */
    @Test
    public void failCascadesToStepsAndPartitionsPreservingTerminalOnes() {
        final JobExecutionImpl jobExecution = execution(job, BatchStatus.STARTED, 60);
        final StepExecutionImpl completedStep = step(jobExecution, "step1", BatchStatus.COMPLETED, "GOOD");
        final StepExecutionImpl runningStep = step(jobExecution, "step2", BatchStatus.STARTED, null);
        final PartitionExecutionImpl donePartition = partition(runningStep, 0, BatchStatus.COMPLETED, "P_GOOD");
        final PartitionExecutionImpl livePartition = partition(runningStep, 1, BatchStatus.STARTED, null);
        final Date completedStepEnd = completedStep.getEndTime();

        repository.failJobExecutions(Collections.singletonList(jobExecution.getExecutionId()),
                null, "FAILED_BY_ADMIN", new Date());

        assertEquals(BatchStatus.COMPLETED, completedStep.getBatchStatus(), "a completed step must be left alone");
        assertEquals("GOOD", completedStep.getExitStatus());
        assertEquals(completedStepEnd, completedStep.getEndTime());

        assertEquals(BatchStatus.FAILED, runningStep.getBatchStatus());
        assertEquals("FAILED_BY_ADMIN", runningStep.getExitStatus());
        assertNotNull(runningStep.getEndTime());

        assertEquals(BatchStatus.COMPLETED, donePartition.getBatchStatus(), "a completed partition must be left alone");
        assertEquals("P_GOOD", donePartition.getExitStatus());

        assertEquals(BatchStatus.FAILED, livePartition.getBatchStatus());
        assertEquals("FAILED_BY_ADMIN", livePartition.getExitStatus());
    }

    @Test
    public void failReportsNotFoundNotInScopeAndInvalidStatus() {
        final JobExecutionImpl started = execution(job, BatchStatus.STARTED, 60);
        final JobExecutionImpl completed = execution(job, BatchStatus.COMPLETED, 60);
        final JobExecutionImpl elsewhere = execution(otherJob, BatchStatus.STARTED, 60);

        final Map<Long, FailOutcome> outcomes = repository.failJobExecutions(
                Arrays.asList(started.getExecutionId(), completed.getExecutionId(),
                        elsewhere.getExecutionId(), 999_999L),
                JOB, "FAILED_BY_ADMIN", new Date());

        assertEquals(FailOutcome.Kind.FAILED, outcomes.get(started.getExecutionId()).kind());

        assertEquals(FailOutcome.Kind.INVALID_STATUS, outcomes.get(completed.getExecutionId()).kind());
        assertEquals(BatchStatus.COMPLETED, outcomes.get(completed.getExecutionId()).observedStatus());
        assertEquals(BatchStatus.COMPLETED, completed.getBatchStatus(), "must be untouched");

        assertEquals(FailOutcome.Kind.NOT_IN_SCOPE, outcomes.get(elsewhere.getExecutionId()).kind());
        assertEquals(BatchStatus.STARTED, elsewhere.getBatchStatus(), "must be untouched");

        assertEquals(FailOutcome.Kind.NOT_FOUND, outcomes.get(999_999L).kind());
    }

    /** With no scope the owning job name is not consulted, matching an unscoped operator. */
    @Test
    public void failWithoutScopeIgnoresTheOwningJob() {
        final JobExecutionImpl elsewhere = execution(otherJob, BatchStatus.STARTED, 60);

        final Map<Long, FailOutcome> outcomes = repository.failJobExecutions(
                Collections.singletonList(elsewhere.getExecutionId()), null, "FAILED_BY_ADMIN", new Date());

        assertEquals(FailOutcome.Kind.FAILED, outcomes.get(elsewhere.getExecutionId()).kind());
    }

    /** Failing twice changes nothing the second time. */
    @Test
    public void failIsIdempotent() {
        final JobExecutionImpl jobExecution = execution(job, BatchStatus.STARTED, 60);
        final List<Long> ids = Collections.singletonList(jobExecution.getExecutionId());

        repository.failJobExecutions(ids, null, "FIRST", new Date());
        final Date firstEnd = jobExecution.getEndTime();

        final Map<Long, FailOutcome> second = repository.failJobExecutions(ids, null, "SECOND", new Date());

        assertEquals(FailOutcome.Kind.INVALID_STATUS, second.get(jobExecution.getExecutionId()).kind());
        assertEquals(BatchStatus.FAILED, second.get(jobExecution.getExecutionId()).observedStatus());
        assertEquals("FIRST", jobExecution.getExitStatus(), "the second call must change nothing");
        assertEquals(firstEnd, jobExecution.getEndTime());
    }

    /**
     * Two callers racing on the same execution: exactly one wins, and the loser is told the
     * execution moved rather than being silently dropped.
     * <p>
     * The race is made deterministic through the package-private seam on
     * {@link InMemoryRepository#failJobExecutions0}: both threads complete the read-and-classify
     * pass, meet at a barrier, and only then contend for the write lock. 
     */
    @Test
    public void concurrentFailYieldsOneWinnerAndOneConcurrentModification() throws Exception {
        final JobExecutionImpl jobExecution = execution(job, BatchStatus.STARTED, 60);
        final List<Long> ids = Collections.singletonList(jobExecution.getExecutionId());

        final CyclicBarrier bothHaveRead = new CyclicBarrier(2);
        final CountDownLatch done = new CountDownLatch(2);
        final AtomicReference<Map<Long, FailOutcome>> first = new AtomicReference<Map<Long, FailOutcome>>();
        final AtomicReference<Map<Long, FailOutcome>> second = new AtomicReference<Map<Long, FailOutcome>>();

        final Runnable afterRead = () -> {
            try {
                bothHaveRead.await(10, TimeUnit.SECONDS);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        };

        final Thread t1 = new Thread(() -> {
            try {
                first.set(repository.failJobExecutions0(ids, null, "A", new Date(), afterRead));
            } finally {
                done.countDown();
            }
        });
        final Thread t2 = new Thread(() -> {
            try {
                second.set(repository.failJobExecutions0(ids, null, "B", new Date(), afterRead));
            } finally {
                done.countDown();
            }
        });
        t1.start();
        t2.start();
        assertTrue(done.await(20, TimeUnit.SECONDS), "both threads should finish");
        t1.join();
        t2.join();

        final FailOutcome.Kind kind1 = first.get().get(jobExecution.getExecutionId()).kind();
        final FailOutcome.Kind kind2 = second.get().get(jobExecution.getExecutionId()).kind();

        assertEquals(1, (kind1 == FailOutcome.Kind.FAILED ? 1 : 0) + (kind2 == FailOutcome.Kind.FAILED ? 1 : 0),
                "exactly one caller should win: " + kind1 + " / " + kind2);
        assertEquals(1,
                (kind1 == FailOutcome.Kind.CONCURRENTLY_MODIFIED ? 1 : 0)
                        + (kind2 == FailOutcome.Kind.CONCURRENTLY_MODIFIED ? 1 : 0),
                "the loser should be told the execution moved: " + kind1 + " / " + kind2);
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void failWithNoIdsDoesNothing() {
        assertTrue(repository.failJobExecutions(Collections.<Long>emptyList(), null, "X", new Date()).isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // repositories that cannot support these operations
    // ---------------------------------------------------------------------------------------

    @Test
    public void cacheBackedRepositoriesRefuseByDefault() {
        final JobRepository unsupported = new AbstractPersistentRepositoryStub();

        assertTrue(assertThrows(UnsupportedOperationException.class,
                () -> unsupported.countStaleExecutions(JOB, new Date())).getMessage().startsWith("JBERET000665"));
        assertThrows(UnsupportedOperationException.class,
                () -> unsupported.findStaleExecutionIds(JOB, new Date(), 0, 10));
        assertThrows(UnsupportedOperationException.class,
                () -> unsupported.failJobExecutions(Collections.singletonList(1L), null, "X", new Date()));
    }

    /** Confirms the singleton accessor and the factory hand back usable repositories. */
    @Test
    public void bothInMemoryFactoriesSupportTheOperations() {
        assertSame(InMemoryRepository.getInstance(), InMemoryRepository.getInstance());
        assertEquals(0L, InMemoryRepository.create().countStaleExecutions("nobody", new Date()));
    }

    /** A minimal concrete {@code AbstractPersistentRepository}, for the refusal test above. */
    private static final class AbstractPersistentRepositoryStub extends AbstractPersistentRepository {
        @Override
        void insertJobInstance(final JobInstanceImpl jobInstance) {
        }

        @Override
        void insertJobExecution(final JobExecutionImpl jobExecution) {
        }

        @Override
        void insertStepExecution(final StepExecutionImpl stepExecution, final JobExecutionImpl jobExecution) {
        }

        @Override
        List<jakarta.batch.runtime.StepExecution> selectStepExecutions(final Long jobExecutionId,
                                                                       final ClassLoader classLoader) {
            return Collections.emptyList();
        }

        @Override
        public List<jakarta.batch.runtime.JobInstance> getJobInstances(final String jobName) {
            return Collections.emptyList();
        }

        @Override
        public int getJobInstanceCount(final String jobName) {
            return 0;
        }

        @Override
        public List<Long> getRunningExecutions(final String jobName) {
            return Collections.emptyList();
        }

        @Override
        public List<jakarta.batch.runtime.JobExecution> getJobExecutions(final jakarta.batch.runtime.JobInstance jobInstance) {
            return Collections.emptyList();
        }

        @Override
        public List<Long> getJobExecutionsByJob(final String jobName) {
            return Collections.emptyList();
        }

        @Override
        public List<Long> getJobExecutionsByJob(final String jobName, final Integer limit) {
            return Collections.emptyList();
        }

        @Override
        public void updateStepExecution(final jakarta.batch.runtime.StepExecution stepExecution) {
        }

        @Override
        public int countStepStartTimes(final String stepName, final long jobInstanceId) {
            return 0;
        }
    }
}
