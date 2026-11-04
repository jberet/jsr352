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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.runtime.BatchStatus;
import org.jberet._private.BatchMessages;
import org.jberet.operations.FailExecutionsResult.SkipReason;
import org.jberet.operations.FailExecutionsResult.SkippedExecution;
import org.jberet.repository.FailOutcome;
import org.jberet.repository.JobRepository;
import org.jberet.spi.BatchEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the two administrative operations {@link AbstractJobOperator} adds, exercised
 * against {@link FakeJobRepository} so that the operator's own logic — argument validation, cut-off
 * arithmetic, de-duplication, the local-liveness screen and outcome translation — is tested
 * without a database.
 */
public class AbstractJobOperatorAdminTest {

    private static final String JOB = "adminOpsJob";

    private FakeJobRepository repository;
    private TestOperator operator;

    /**
     * The smallest concrete operator there is. The administrative methods only reach the batch
     * environment through {@link AbstractJobOperator#getJobRepository()}, which is overridden here,
     * so no environment is needed.
     */
    private static final class TestOperator extends AbstractJobOperator {
        private final JobRepository repository;
        private String scopeJobName;

        TestOperator(final JobRepository repository) {
            this.repository = repository;
        }

        @Override
        public BatchEnvironment getBatchEnvironment() {
            throw new UnsupportedOperationException("no batch environment in this test");
        }

        @Override
        public JobRepository getJobRepository() {
            return repository;
        }

        @Override
        protected String scopeJobName() {
            return scopeJobName;
        }
    }

    @BeforeEach
    public void setUp() {
        repository = new FakeJobRepository();
        repository.knownJobs.add(JOB);
        operator = new TestOperator(repository);
    }

    private static List<Long> ids(final long... values) {
        final List<Long> list = new ArrayList<Long>(values.length);
        for (final long value : values) {
            list.add(value);
        }
        return list;
    }

    // ---------------------------------------------------------------------------------------
    // listExecutionsNotUpdatedInMinutes
    // ---------------------------------------------------------------------------------------

    /** U1: three stale executions are returned as one full page. */
    @Test
    public void listReturnsAllMatchesOnOnePage() {
        repository.staleIds.addAll(ids(11L, 22L, 33L));

        final StaleExecutionsPage page = operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 0, 10);

        assertEquals(ids(11L, 22L, 33L), page.getExecutionIds());
        assertEquals(3L, page.getTotalCount());
        assertEquals(0, page.getStart());
        assertEquals(3, page.getCount());
        assertFalse(page.hasMore());
        assertEquals(JOB, repository.recordedCountJobName);
    }

    /** U2: a known job with nothing stale yields an empty page, not an exception. */
    @Test
    public void listWithNoMatchesForKnownJobIsEmpty() {
        final StaleExecutionsPage page = operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 0, 10);

        assertTrue(page.getExecutionIds().isEmpty());
        assertEquals(0L, page.getTotalCount());
        assertEquals(0, page.getCount());
        assertFalse(page.hasMore());
        //with a zero count there is nothing to page over, so the second query is not issued
        assertEquals(0, repository.findStaleCalls);
    }

    /** U3: no matches and no such job is an error, matching getRunningExecutions()'s contract. */
    @Test
    public void listForUnknownJobThrows() {
        assertThrows(NoSuchJobException.class,
                () -> operator.listExecutionsNotUpdatedInMinutes("noSuchJob", 30, 0, 10));
    }

    /** U4: zero minutes means "every STARTED or STOPPING execution", so the cut-off is now. */
    @Test
    public void listWithZeroMinutesUsesNowAsCutoff() {
        repository.staleIds.addAll(ids(1L, 2L));
        final long before = System.currentTimeMillis();

        final StaleExecutionsPage page = operator.listExecutionsNotUpdatedInMinutes(JOB, 0, 0, 10);

        final long after = System.currentTimeMillis();
        assertEquals(2, page.getCount());
        final long cutoff = repository.recordedCountNotUpdatedSince.getTime();
        assertTrue(cutoff >= before && cutoff <= after,
                "cut-off " + cutoff + " should be between " + before + " and " + after);
    }

    /** U5: out-of-range minutes and pagination arguments are rejected. */
    @Test
    public void listRejectsOutOfRangeArguments() {
        assertEquals("JBERET000662: Invalid number of minutes: -1. It must be greater than or equal to 0.",
                assertThrows(IllegalArgumentException.class,
                        () -> operator.listExecutionsNotUpdatedInMinutes(JOB, -1, 0, 10)).getMessage());

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> operator.listExecutionsNotUpdatedInMinutes(JOB, 30, -1, 10))
                .getMessage().startsWith("JBERET000663"));

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 0, 0))
                .getMessage().startsWith("JBERET000663"));

        assertEquals(0, repository.countStaleCalls, "nothing should reach the repository");
    }

    /** U6: a null job name is NoSuchJobException, as it is for every other operator method. */
    @Test
    public void listRejectsNullJobName() {
        assertThrows(NoSuchJobException.class,
                () -> operator.listExecutionsNotUpdatedInMinutes(null, 30, 0, 10));
        assertEquals(0, repository.countStaleCalls);
    }

    /** U7: the cut-off handed to the repository is now minus the requested minutes. */
    @Test
    public void listComputesCutoffFromTheJvmClock() {
        repository.staleIds.add(7L);
        final long expected = System.currentTimeMillis() - 30 * 60_000L;

        operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 0, 10);

        final long actual = repository.recordedCountNotUpdatedSince.getTime();
        assertTrue(Math.abs(actual - expected) < 1000L,
                "cut-off " + actual + " should be within a second of " + expected);
        //both queries must agree on the cut-off, or the count and the page describe different sets
        assertEquals(repository.recordedCountNotUpdatedSince, repository.recordedFindNotUpdatedSince);
    }

    /** U8: offset and count reach the repository verbatim. */
    @Test
    public void listPassesPaginationThroughVerbatim() {
        for (long i = 1; i <= 30; i++) {
            repository.staleIds.add(i);
        }

        final StaleExecutionsPage page = operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 10, 10);

        assertEquals(10, repository.recordedFindStart);
        assertEquals(10, repository.recordedFindCount);
        assertEquals(ids(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L), page.getExecutionIds());
        assertEquals(30L, page.getTotalCount());
        assertEquals(10, page.getStart());
        assertTrue(page.hasMore());
    }

    /** U9: a start beyond the end yields an empty page that still reports the true total. */
    @Test
    public void listWithStartBeyondTotalIsEmptyButKeepsTheTotal() {
        repository.staleIds.addAll(ids(1L, 2L, 3L));

        final StaleExecutionsPage page = operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 100, 10);

        assertTrue(page.getExecutionIds().isEmpty());
        assertEquals(3L, page.getTotalCount());
        assertEquals(100, page.getStart());
        assertEquals(0, page.getCount());
        assertFalse(page.hasMore());
    }

    /** U10: a count larger than the remainder returns the remainder, and getCount() reflects that. */
    @Test
    public void listReturnsRemainderWhenCountExceedsIt() {
        repository.staleIds.addAll(ids(1L, 2L, 3L, 4L, 5L));

        final StaleExecutionsPage page = operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 3, 10);

        assertEquals(ids(4L, 5L), page.getExecutionIds());
        assertEquals(2, page.getCount());
        assertEquals(5L, page.getTotalCount());
        assertFalse(page.hasMore());
    }

    /** U11: a repository that cannot support the query propagates its refusal unwrapped. */
    @Test
    public void listPropagatesUnsupportedOperation() {
        repository.staleQueryError = BatchMessages.MESSAGES.adminOperationNotSupported("FakeJobRepository");

        final UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> operator.listExecutionsNotUpdatedInMinutes(JOB, 30, 0, 10));
        assertTrue(e.getMessage().startsWith("JBERET000665"));
    }

    // ---------------------------------------------------------------------------------------
    // failJobExecutions
    // ---------------------------------------------------------------------------------------

    /** U12: every execution eligible means every execution failed and nothing skipped. */
    @Test
    public void failAllEligible() {
        final FailExecutionsResult result = operator.failJobExecutions(ids(1L, 2L, 3L));

        assertEquals(3, result.getFailedCount());
        assertEquals(ids(1L, 2L, 3L), result.getFailedExecutionIds());
        assertEquals(0, result.getSkippedCount());
        assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, repository.recordedFailExitStatus);
    }

    /** U13: a mixed batch reports one failure and two skips with distinct reasons. */
    @Test
    public void failMixedBatchReportsDistinctSkipReasons() {
        repository.failOutcomes.put(2L, FailOutcome.invalidStatus(BatchStatus.COMPLETED));
        repository.failOutcomes.put(3L, FailOutcome.notFound());

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L, 2L, 3L));

        assertEquals(1, result.getFailedCount());
        assertEquals(Collections.singletonList(1L), result.getFailedExecutionIds());
        assertEquals(2, result.getSkippedCount());

        final SkippedExecution completed = result.getSkipped().get(0);
        assertEquals(2L, completed.getExecutionId());
        assertEquals(SkipReason.INVALID_BATCH_STATUS, completed.getReason());
        assertEquals(BatchStatus.COMPLETED, completed.getObservedStatus());
        assertTrue(completed.getMessage().contains("COMPLETED"), completed.getMessage());

        final SkippedExecution missing = result.getSkipped().get(1);
        assertEquals(3L, missing.getExecutionId());
        assertEquals(SkipReason.NOT_FOUND, missing.getReason());
    }

    /** U14: nothing eligible means nothing failed; the ids are still reported individually. */
    @Test
    public void failNoneEligible() {
        for (long id = 1; id <= 3; id++) {
            repository.failOutcomes.put(id, FailOutcome.invalidStatus(BatchStatus.COMPLETED));
        }

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L, 2L, 3L));

        assertEquals(0, result.getFailedCount());
        assertEquals(3, result.getSkippedCount());
        for (final SkippedExecution skipped : result.getSkipped()) {
            assertEquals(SkipReason.INVALID_BATCH_STATUS, skipped.getReason());
        }
    }

    /** U15: duplicates are collapsed, and the first occurrence fixes the order. */
    @Test
    public void failCollapsesDuplicates() {
        final FailExecutionsResult result = operator.failJobExecutions(ids(5L, 7L, 5L, 7L, 5L));

        assertEquals(ids(5L, 7L), repository.recordedFailIds);
        assertEquals(ids(5L, 7L), result.getFailedExecutionIds());
    }

    /** U16: an empty request never reaches the repository. */
    @Test
    public void failWithEmptyListDoesNothing() {
        final FailExecutionsResult result = operator.failJobExecutions(Collections.<Long>emptyList());

        assertEquals(0, result.getFailedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, repository.failCalls);
    }

    /** U17: the batch cap is enforced on the de-duplicated set, before any work is done. */
    @Test
    public void failRejectsOversizedBatch() {
        final List<Long> many = new ArrayList<Long>();
        for (long id = 1; id <= 11; id++) {
            many.add(id);
        }

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> operator.failJobExecutions(many, FailOptions.defaults().withMaxBatchSize(10)));
        assertTrue(e.getMessage().startsWith("JBERET000664"), e.getMessage());
        assertEquals(0, repository.failCalls);
    }

    /** U17b: the cap applies after de-duplication, so a list of repeats is not over it. */
    @Test
    public void failCapCountsDistinctIdsOnly() {
        final List<Long> repeats = new ArrayList<Long>();
        for (int i = 0; i < 20; i++) {
            repeats.add(42L);
        }

        final FailExecutionsResult result =
                operator.failJobExecutions(repeats, FailOptions.defaults().withMaxBatchSize(1));

        assertEquals(1, result.getFailedCount());
    }

    /** U18: a repository failure becomes FailJobExecutionsException carrying every attempted id. */
    @Test
    public void failWrapsRepositoryFailure() {
        final RuntimeException cause =
                BatchMessages.MESSAGES.failToFailJobExecutions(new IllegalStateException("boom"), ids(1L, 2L));
        repository.failJobExecutionsError = cause;

        final FailJobExecutionsException e = assertThrows(FailJobExecutionsException.class,
                () -> operator.failJobExecutions(ids(1L, 2L)));

        assertEquals(ids(1L, 2L), e.getRequestedExecutionIds());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().startsWith("JBERET000666"), e.getMessage());
    }

    /** U18b: a repository that cannot support the operation is a configuration error, not a rollback. */
    @Test
    public void failPropagatesUnsupportedOperationUnwrapped() {
        repository.failJobExecutionsError =
                BatchMessages.MESSAGES.adminOperationNotSupported("FakeJobRepository");

        assertThrows(UnsupportedOperationException.class, () -> operator.failJobExecutions(ids(1L)));
    }

    /** U19: an execution alive in this JVM is screened out before the repository is called. */
    @Test
    public void failSkipsLocallyRunningExecution() {
        repository.addExecution(1L, JOB, BatchStatus.STARTED, true);
        repository.addExecution(2L, JOB, BatchStatus.STARTED, false);

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L, 2L));

        assertEquals(Collections.singletonList(2L), repository.recordedFailIds,
                "the live execution must never reach the repository");
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getSkippedCount());
        final SkippedExecution skipped = result.getSkipped().get(0);
        assertEquals(1L, skipped.getExecutionId());
        assertEquals(SkipReason.RUNNING_LOCALLY, skipped.getReason());
    }

    /** U19b: when every id is screened out, the repository is not called at all. */
    @Test
    public void failWithOnlyLocallyRunningExecutionsSkipsTheRepository() {
        repository.addExecution(1L, JOB, BatchStatus.STARTED, true);

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L));

        assertEquals(0, repository.failCalls);
        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getSkippedCount());
    }

    /** U20: with screening off, the same live execution is handed to the repository. */
    @Test
    public void failWithoutLocalScreeningPassesLiveExecutionThrough() {
        repository.addExecution(1L, JOB, BatchStatus.STARTED, true);

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L),
                FailOptions.defaults().withSkipLocallyRunning(false));

        assertEquals(Collections.singletonList(1L), repository.recordedFailIds);
        assertEquals(1, result.getFailedCount());
    }

    /** U21: a custom exit status reaches the repository unchanged. */
    @Test
    public void failPassesCustomExitStatusThrough() {
        operator.failJobExecutions(ids(1L), FailOptions.defaults().withExitStatus("FAILED_BY_REAPER"));

        assertEquals("FAILED_BY_REAPER", repository.recordedFailExitStatus);
    }

    /** U22: failing twice is idempotent — the second call changes nothing and reports skips. */
    @Test
    public void failIsIdempotent() {
        final FailExecutionsResult first = operator.failJobExecutions(ids(1L, 2L));
        assertEquals(2, first.getFailedCount());

        //the repository now reports them as already terminal, exactly as a real one would
        repository.failOutcomes.put(1L, FailOutcome.invalidStatus(BatchStatus.FAILED));
        repository.failOutcomes.put(2L, FailOutcome.invalidStatus(BatchStatus.FAILED));

        final FailExecutionsResult second = operator.failJobExecutions(ids(1L, 2L));
        assertEquals(0, second.getFailedCount());
        assertEquals(2, second.getSkippedCount());
        for (final SkippedExecution skipped : second.getSkipped()) {
            assertEquals(SkipReason.INVALID_BATCH_STATUS, skipped.getReason());
            assertEquals(BatchStatus.FAILED, skipped.getObservedStatus());
        }
    }

    /** An execution that moved between the read and the write is reported, not silently dropped. */
    @Test
    public void failReportsConcurrentModification() {
        repository.failOutcomes.put(1L, FailOutcome.concurrentlyModified(BatchStatus.STARTED));

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L));

        assertEquals(0, result.getFailedCount());
        assertEquals(SkipReason.CONCURRENTLY_MODIFIED, result.getSkipped().get(0).getReason());
        assertEquals(BatchStatus.STARTED, result.getSkipped().get(0).getObservedStatus());
    }

    /** An execution outside the operator's scope is reported as such. */
    @Test
    public void failReportsOutOfScopeExecution() {
        operator.scopeJobName = JOB;
        repository.failOutcomes.put(1L, FailOutcome.notInScope());

        final FailExecutionsResult result = operator.failJobExecutions(ids(1L));

        assertEquals(JOB, repository.recordedFailJobName, "the scope must reach the repository");
        assertEquals(0, result.getFailedCount());
        assertEquals(SkipReason.NOT_IN_SCOPE, result.getSkipped().get(0).getReason());
    }

    /** A null id list is an argument error; nulls inside the list are ignored. */
    @Test
    public void failValidatesTheIdList() {
        assertThrows(IllegalArgumentException.class, () -> operator.failJobExecutions(null));

        final FailExecutionsResult result = operator.failJobExecutions(Arrays.asList(1L, null, 2L));
        assertEquals(ids(1L, 2L), repository.recordedFailIds);
        assertEquals(2, result.getFailedCount());
    }

    /** Null options fall back to the defaults rather than failing. */
    @Test
    public void failAcceptsNullOptions() {
        final FailExecutionsResult result = operator.failJobExecutions(ids(1L), null);

        assertNotNull(result);
        assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, repository.recordedFailExitStatus);
    }
}
