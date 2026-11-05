/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.transaction.Status;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.operations.ExtendedJobOperator;
import org.jberet.operations.FailExecutionsResult;
import org.jberet.operations.FailExecutionsResult.SkipReason;
import org.jberet.operations.FailOptions;
import org.jberet.operations.JobOperatorImpl;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.FailOutcome;
import org.jberet.repository.JdbcRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.tx.LocalTransactionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Java SE coverage for {@link ExtendedJobOperator#failJobExecutions}.
 * <p>
 * It has two halves. One runs against a private H2 database through a data source that can be told
 * to make one statement fail, which is how the atomicity requirement is tested: in Java SE the
 * transaction manager is {@code LocalTransactionManager}, whose {@code begin} and {@code commit}
 * move a {@code ThreadLocal} and touch no connection, so an implementation that wrapped the work
 * in {@code tm.begin()}/{@code tm.commit()} would pass every other test here and fail
 * {@link #nothingIsChangedWhenTheTransactionFails()}. The other half runs through
 * {@code BatchRuntime.getJobOperator()} against real, parked job executions, which is the only way
 * to exercise the local-liveness screen and the write-through to the in-memory cache.
 * <p>
 * This class uses its own job, {@code org.jberet.se.test.adminOpsFail}, so that it cannot disturb
 * {@code StaleExecutionsSETest}, which shares the same H2 job repository, nor {@code JobDataTest},
 * which asserts against the job instances {@code Batchlet1Test} leaves behind.
 */
public class FailJobExecutionsSETest {

    private static final String JOB = "org.jberet.se.test.adminOpsFail";
    private static final String REAPER_JOB = "org.jberet.se.test.adminOpsReaper";
    private static final long AWAIT_SECONDS = 60;

    /**
     * A database of this test's own, so that injecting a fault cannot disturb the job repository
     * the Java SE runtime is using. It lives only as long as this JVM, which surefire forks per
     * test class.
     */
    private static final String PRIVATE_DB_URL = "jdbc:h2:mem:adminOpsFailSETest;DB_CLOSE_DELAY=-1";

    /** Matches only the last of the three statements the fail operation issues. */
    private static final String FAIL_JOB_EXECUTION_SQL = "UPDATE JOB_EXECUTION SET BATCHSTATUS='FAILED'";

    /** How far back a fixture execution's last progress marker is pushed: two hours. */
    private static final long STALE_BY_MILLIS = 2 * 60 * 60 * 1000L;

    private static AdminOpsFixtures.FaultInjectingDataSource dataSource;
    private static JdbcRepository repository;
    private static Connection keepAlive;

    private final JobOperator operator = BatchRuntime.getJobOperator();

    @BeforeAll
    public static void createPrivateRepository() throws Exception {
        //hold one connection open so the in-memory database survives between the others
        keepAlive = DriverManager.getConnection(PRIVATE_DB_URL);
        dataSource = new AdminOpsFixtures.FaultInjectingDataSource(PRIVATE_DB_URL);
        repository = new JdbcRepository(dataSource, new Properties());
        //the same transaction manager the Java SE runtime wires in, so the repository is making
        //the real decision about who owns the transaction
        repository.setTransactionManager(LocalTransactionManager.getInstance());
    }

    @AfterAll
    public static void releaseEverything() throws Exception {
        AdminOpsTargetBatchlet.release.countDown();
        if (keepAlive != null) {
            keepAlive.close();
        }
    }

    // ---------------------------------------------------------------------------------------
    // fixtures in the private database
    // ---------------------------------------------------------------------------------------

    /**
     * Creates a job execution that looks like one whose JVM died: {@code STARTED} in the database,
     * with a step execution per entry of {@code stepStatuses}, and with its job instance no longer
     * holding an unsubstituted job, which is what marks an execution as no longer running here.
     *
     * @param jobName      the job name
     * @param stepStatuses the batch status each step execution should end up in
     *
     * @return the job execution
     */
    private static JobExecutionImpl zombie(final String jobName, final BatchStatus... stepStatuses) {
        final Job job = new JobBuilder(jobName).build();
        repository.addJob(new ApplicationAndJobName(null, jobName), job);
        final JobInstanceImpl jobInstance =
                repository.createJobInstance(job, null, FailJobExecutionsSETest.class.getClassLoader());
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, null);
        jobExecution.setBatchStatus(BatchStatus.STARTED);
        repository.updateJobExecution(jobExecution, true, false);

        int i = 0;
        for (final BatchStatus stepStatus : stepStatuses) {
            final StepExecutionImpl stepExecution = repository.createStepExecution("step" + (++i));
            stepExecution.setBatchStatus(BatchStatus.STARTED);
            repository.addStepExecution(jobExecution, stepExecution);
            if (stepStatus != BatchStatus.STARTED) {
                stepExecution.setBatchStatus(stepStatus);
                stepExecution.setExitStatus(stepStatus.name());
                repository.updateStepExecution(stepExecution);
            }
        }
        //Back-date the progress marker, which is the only thing that makes an execution look
        //stale. It has to be done in SQL: every repository write stamps LASTUPDATEDTIME with the
        //current time, so there is no way to persist an old one through the repository API. This
        //is the same trick the management-level integration tests use.
        backdateLastUpdatedTime(jobExecution, STALE_BY_MILLIS);

        //no runtime is driving this execution any more
        jobInstance.setUnsubstitutedJob(null);
        return jobExecution;
    }

    /** Rewrites an execution's last-updated time, in the database and in the cached object. */
    private static void backdateLastUpdatedTime(final JobExecutionImpl jobExecution, final long byMillis) {
        final long when = System.currentTimeMillis() - byMillis;
        try (Connection connection = DriverManager.getConnection(PRIVATE_DB_URL);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE JOB_EXECUTION SET LASTUPDATEDTIME=?, STARTTIME=? WHERE JOBEXECUTIONID=?")) {
            statement.setTimestamp(1, new java.sql.Timestamp(when));
            statement.setTimestamp(2, new java.sql.Timestamp(when));
            statement.setLong(3, jobExecution.getExecutionId());
            statement.executeUpdate();
        } catch (final Exception e) {
            throw new IllegalStateException("failed to back-date execution "
                    + jobExecution.getExecutionId(), e);
        }
        jobExecution.setLastUpdatedTime(when);
    }

    private static String readJobStatus(final long executionId) throws Exception {
        return readOne("SELECT BATCHSTATUS FROM JOB_EXECUTION WHERE JOBEXECUTIONID=?", executionId);
    }

    private static String readJobExitStatus(final long executionId) throws Exception {
        return readOne("SELECT EXITSTATUS FROM JOB_EXECUTION WHERE JOBEXECUTIONID=?", executionId);
    }

    private static String readJobEndTime(final long executionId) throws Exception {
        return readOne("SELECT ENDTIME FROM JOB_EXECUTION WHERE JOBEXECUTIONID=?", executionId);
    }

    private static String readStepStatus(final long stepExecutionId) throws Exception {
        return readOne("SELECT BATCHSTATUS FROM STEP_EXECUTION WHERE STEPEXECUTIONID=?", stepExecutionId);
    }

    private static String readStepExitStatus(final long stepExecutionId) throws Exception {
        return readOne("SELECT EXITSTATUS FROM STEP_EXECUTION WHERE STEPEXECUTIONID=?", stepExecutionId);
    }

    private static String readPartitionStatus(final long stepExecutionId, final int partitionId) throws Exception {
        return readOne("SELECT BATCHSTATUS FROM PARTITION_EXECUTION WHERE STEPEXECUTIONID=? "
                + "AND PARTITIONEXECUTIONID=" + partitionId, stepExecutionId);
    }

    /** Reads one value straight from the database, bypassing every cache the repository keeps. */
    private static String readOne(final String sql, final long id) throws Exception {
        try (Connection connection = DriverManager.getConnection(PRIVATE_DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // the atomicity test
    // ---------------------------------------------------------------------------------------

    /**
     * The exit criterion for this feature. With a fault injected into the last of the three
     * statements, every earlier write in the same call must be undone.
     * <p>
     * The step execution update has already run by then, so if the operation were not inside a
     * real JDBC transaction the step would be left {@code FAILED} under a {@code STARTED} job
     * execution — a corrupt state, and exactly what a no-op {@code LocalTransactionManager} would
     * produce.
     */
    @Test
    public void nothingIsChangedWhenTheTransactionFails() throws Exception {
        final JobExecutionImpl jobExecution = zombie(JOB, BatchStatus.STARTED);
        final long executionId = jobExecution.getExecutionId();
        final long stepExecutionId = jobExecution.getStepExecutions().get(0).getStepExecutionId();
        assertEquals("STARTED", readJobStatus(executionId));
        assertEquals("STARTED", readStepStatus(stepExecutionId));

        final int faultsBefore = dataSource.faultsFired;
        dataSource.failOnSqlContaining = FAIL_JOB_EXECUTION_SQL;
        try {
            final BatchRuntimeException e = assertThrows(BatchRuntimeException.class,
                    () -> repository.failJobExecutions(Collections.singletonList(executionId),
                            null, "FAILED_BY_ADMIN", new Date()));
            assertTrue(e.getMessage().startsWith("JBERET000666"), e.getMessage());
        } finally {
            dataSource.failOnSqlContaining = null;
        }
        assertEquals(faultsBefore + 1, dataSource.faultsFired, "the fault should have fired exactly once");

        assertEquals("STARTED", readJobStatus(executionId), "the job execution must be unchanged");
        assertNull(readJobExitStatus(executionId), "no exit status should have been written");
        assertNull(readJobEndTime(executionId), "no end time should have been written");
        assertEquals("STARTED", readStepStatus(stepExecutionId),
                "the step execution update must have been rolled back");
        assertNull(readStepExitStatus(stepExecutionId));
    }

    /**
     * The same execution, with no fault injected, really is failed — otherwise the test above
     * could pass simply because nothing was ever written.
     */
    @Test
    public void theSameOperationSucceedsWithoutTheFault() throws Exception {
        final JobExecutionImpl jobExecution = zombie(JOB, BatchStatus.STARTED);
        final long executionId = jobExecution.getExecutionId();
        final long stepExecutionId = jobExecution.getStepExecutions().get(0).getStepExecutionId();

        final Map<Long, FailOutcome> outcomes = repository.failJobExecutions(
                Collections.singletonList(executionId), null, "FAILED_BY_ADMIN", new Date());

        assertEquals(FailOutcome.Kind.FAILED, outcomes.get(executionId).kind());
        assertEquals("FAILED", readJobStatus(executionId));
        assertEquals("FAILED_BY_ADMIN", readJobExitStatus(executionId));
        assertNotNull(readJobEndTime(executionId));
        assertEquals("FAILED", readStepStatus(stepExecutionId));
        assertEquals("FAILED_BY_ADMIN", readStepExitStatus(stepExecutionId));
    }

    /**
     * The trap this feature was most likely to fall into, made explicit.
     * <p>
     * {@code LocalTransactionManager.begin()} sets a {@code ThreadLocal} to
     * {@code STATUS_ACTIVE} and touches no connection. An implementation whose guard were just
     * {@code tm.getStatus() == STATUS_ACTIVE} would conclude a container owns the transaction,
     * skip {@code setAutoCommit(false)} entirely, and commit each statement as it ran — so the
     * step execution update would survive the failure of the one after it.
     */
    @Test
    public void atomicityHoldsWhileLocalTransactionManagerClaimsATransactionIsActive() throws Exception {
        final JobExecutionImpl jobExecution = zombie(JOB, BatchStatus.STARTED);
        final long executionId = jobExecution.getExecutionId();
        final long stepExecutionId = jobExecution.getStepExecutions().get(0).getStepExecutionId();

        final LocalTransactionManager tm = LocalTransactionManager.getInstance();
        tm.begin();
        try {
            assertEquals(Status.STATUS_ACTIVE, tm.getStatus(),
                    "the local transaction manager should claim a transaction is active");

            dataSource.failOnSqlContaining = FAIL_JOB_EXECUTION_SQL;
            try {
                assertThrows(BatchRuntimeException.class,
                        () -> repository.failJobExecutions(Collections.singletonList(executionId),
                                null, "FAILED_BY_ADMIN", new Date()));
            } finally {
                dataSource.failOnSqlContaining = null;
            }
        } finally {
            tm.rollback();
        }

        assertEquals("STARTED", readJobStatus(executionId));
        assertEquals("STARTED", readStepStatus(stepExecutionId),
                "the step update must have been rolled back, which only a real JDBC transaction can do");
    }

    /** Failing several executions in one call leaves none of them changed when it rolls back. */
    @Test
    public void aRollbackLeavesEveryRequestedExecutionUnchanged() throws Exception {
        final JobExecutionImpl first = zombie(JOB, BatchStatus.STARTED);
        final JobExecutionImpl second = zombie(JOB, BatchStatus.STARTED);
        final List<Long> ids = Arrays.asList(first.getExecutionId(), second.getExecutionId());

        dataSource.failOnSqlContaining = FAIL_JOB_EXECUTION_SQL;
        try {
            assertThrows(BatchRuntimeException.class,
                    () -> repository.failJobExecutions(ids, null, "FAILED_BY_ADMIN", new Date()));
        } finally {
            dataSource.failOnSqlContaining = null;
        }

        for (final Long id : ids) {
            assertEquals("STARTED", readJobStatus(id), "execution " + id + " must be unchanged");
        }
    }

    // ---------------------------------------------------------------------------------------
    // the cascade, against a real database
    // ---------------------------------------------------------------------------------------

    /** Running steps and partitions are failed; terminal ones keep their status and exit status. */
    @Test
    public void cascadeReachesStepsAndPartitionsAndSparesTerminalOnes() throws Exception {
        final JobExecutionImpl jobExecution = zombie(JOB, BatchStatus.COMPLETED, BatchStatus.STARTED);
        final long executionId = jobExecution.getExecutionId();
        final StepExecutionImpl completedStep = (StepExecutionImpl) jobExecution.getStepExecutions().get(0);
        final StepExecutionImpl runningStep = (StepExecutionImpl) jobExecution.getStepExecutions().get(1);

        final PartitionExecutionImpl donePartition = new PartitionExecutionImpl(0,
                runningStep.getStepExecutionId(), runningStep.getStepName(), BatchStatus.COMPLETED,
                "P_GOOD", null, null, null);
        repository.addPartitionExecution(runningStep, donePartition);
        final PartitionExecutionImpl livePartition = new PartitionExecutionImpl(1,
                runningStep.getStepExecutionId(), runningStep.getStepName(), BatchStatus.STARTED,
                null, null, null, null);
        repository.addPartitionExecution(runningStep, livePartition);

        repository.failJobExecutions(Collections.singletonList(executionId), null, "FAILED_BY_ADMIN", new Date());

        assertEquals("FAILED", readJobStatus(executionId));
        assertEquals("COMPLETED", readStepStatus(completedStep.getStepExecutionId()),
                "a completed step must keep its status");
        assertEquals("COMPLETED", readStepExitStatus(completedStep.getStepExecutionId()),
                "a completed step must keep its exit status");
        assertEquals("FAILED", readStepStatus(runningStep.getStepExecutionId()));
        assertEquals("COMPLETED", readPartitionStatus(runningStep.getStepExecutionId(), 0),
                "a completed partition must keep its status");
        assertEquals("FAILED", readPartitionStatus(runningStep.getStepExecutionId(), 1));
    }

    /** Each ineligible id gets its own reason, and none of them stops the eligible ones. */
    @Test
    public void skipReasonsAreReportedPerExecution() throws Exception {
        final JobExecutionImpl eligible = zombie(JOB, BatchStatus.STARTED);
        final JobExecutionImpl terminal = zombie(JOB, BatchStatus.STARTED);
        repository.failJobExecutions(Collections.singletonList(terminal.getExecutionId()),
                null, "EARLIER", new Date());
        final JobExecutionImpl otherJob = zombie("org.jberet.se.test.adminOpsOther", BatchStatus.STARTED);

        final Map<Long, FailOutcome> outcomes = repository.failJobExecutions(
                Arrays.asList(eligible.getExecutionId(), terminal.getExecutionId(),
                        otherJob.getExecutionId(), 9_999_999L),
                JOB, "FAILED_BY_ADMIN", new Date());

        assertEquals(FailOutcome.Kind.FAILED, outcomes.get(eligible.getExecutionId()).kind());

        assertEquals(FailOutcome.Kind.INVALID_STATUS, outcomes.get(terminal.getExecutionId()).kind());
        assertEquals(BatchStatus.FAILED, outcomes.get(terminal.getExecutionId()).observedStatus());
        assertEquals("EARLIER", readJobExitStatus(terminal.getExecutionId()),
                "re-failing an already failed execution must change nothing");

        assertEquals(FailOutcome.Kind.NOT_IN_SCOPE, outcomes.get(otherJob.getExecutionId()).kind());
        assertEquals("STARTED", readJobStatus(otherJob.getExecutionId()), "out of scope means untouched");

        assertEquals(FailOutcome.Kind.NOT_FOUND, outcomes.get(9_999_999L).kind());
    }

    /** More ids than fit in one IN list still run inside the one transaction. */
    @Test
    public void largeBatchesAreChunkedButStillAtomic() throws Exception {
        final int count = 600;      //above the 500-value IN-list chunk size
        final long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = zombie(JOB).getExecutionId();
        }
        final List<Long> idList = new java.util.ArrayList<Long>(count);
        for (final long id : ids) {
            idList.add(id);
        }

        //a fault anywhere in the write phase must undo every chunk read before it
        dataSource.failOnSqlContaining = FAIL_JOB_EXECUTION_SQL;
        try {
            assertThrows(BatchRuntimeException.class,
                    () -> repository.failJobExecutions(idList, null, "FAILED_BY_ADMIN", new Date()));
        } finally {
            dataSource.failOnSqlContaining = null;
        }
        assertEquals("STARTED", readJobStatus(ids[0]));
        assertEquals("STARTED", readJobStatus(ids[count - 1]));

        final Map<Long, FailOutcome> outcomes =
                repository.failJobExecutions(idList, null, "FAILED_BY_ADMIN", new Date());
        assertEquals(count, outcomes.size());
        for (final Long id : idList) {
            assertEquals(FailOutcome.Kind.FAILED, outcomes.get(id).kind(), "execution " + id);
        }
        assertEquals("FAILED", readJobStatus(ids[0]));
        assertEquals("FAILED", readJobStatus(ids[count - 1]));
    }

    /** The staleness query and the fail operation compose, which is the documented workflow. */
    @Test
    public void listThenFailOverTheJdbcRepository() throws Exception {
        final ExtendedJobOperator admin = new JobOperatorImpl(
                AdminOpsFixtures.withRepository(AdminOpsFixtures.seEnvironment(), repository));
        final String jobName = "org.jberet.se.test.adminOpsListThenFail";
        final long first = zombie(jobName).getExecutionId();
        final long second = zombie(jobName).getExecutionId();

        final List<Long> stale = admin.listExecutionsNotUpdatedInMinutes(jobName, 60, 0, 100).getExecutionIds();
        assertEquals(Arrays.asList(first, second), stale);

        final FailExecutionsResult result = admin.failJobExecutions(stale);

        assertEquals(2, result.getFailedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals("FAILED", readJobStatus(first));
        assertEquals("FAILED", readJobStatus(second));
        assertTrue(admin.listExecutionsNotUpdatedInMinutes(jobName, 60, 0, 100).getExecutionIds().isEmpty(),
                "failing them removes them from the stale set");
    }

    // ---------------------------------------------------------------------------------------
    // against the real Java SE runtime and its configured job repository
    // ---------------------------------------------------------------------------------------

    private long startParkedExecution() throws Exception {
        AdminOpsTargetBatchlet.running = new CountDownLatch(1);
        final Properties params = new Properties();
        params.setProperty("block", "true");
        final long executionId = operator.start(JOB, params);
        assertTrue(AdminOpsTargetBatchlet.running.await(AWAIT_SECONDS, TimeUnit.SECONDS),
                "the execution should have reached the batchlet");
        return executionId;
    }

    /** An execution this JVM is actually running is skipped, not failed. */
    @Test
    public void locallyRunningExecutionIsSkipped() throws Exception {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;
        final long executionId = startParkedExecution();

        final FailExecutionsResult result = admin.failJobExecutions(Collections.singletonList(executionId));

        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(SkipReason.RUNNING_LOCALLY, result.getSkipped().get(0).getReason());
        assertEquals(executionId, result.getSkipped().get(0).getExecutionId());
        assertEquals(BatchStatus.STARTED, operator.getJobExecution(executionId).getBatchStatus());
    }

    /**
     * After a successful fail, the operator must report the new state. Without a write-through to
     * the cached execution objects this fails: the execution has a step execution, so
     * {@code JdbcRepository.getJobExecution} answers from the cache without re-reading, and would
     * keep reporting {@code STARTED} for the lifetime of that cached reference.
     */
    @Test
    public void cachedExecutionReflectsTheFailure() throws Exception {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;
        final long executionId = startParkedExecution();
        assertEquals(BatchStatus.STARTED, operator.getJobExecution(executionId).getBatchStatus());
        assertEquals(1, operator.getStepExecutions(executionId).size(),
                "the execution needs a step execution for the cache to be consulted");

        final FailExecutionsResult result = admin.failJobExecutions(Collections.singletonList(executionId),
                FailOptions.defaults().withSkipLocallyRunning(false));
        assertEquals(1, result.getFailedCount());

        final JobExecution jobExecution = operator.getJobExecution(executionId);
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus(),
                "the cached job execution must reflect the committed change");
        assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, jobExecution.getExitStatus());
        assertNotNull(jobExecution.getEndTime());

        for (final StepExecution stepExecution : operator.getStepExecutions(executionId)) {
            assertEquals(BatchStatus.FAILED, stepExecution.getBatchStatus(),
                    "the cached step execution must reflect the committed change");
            assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, stepExecution.getExitStatus());
        }
    }

    // ---------------------------------------------------------------------------------------
    // the worked reaper example
    // ---------------------------------------------------------------------------------------

    private String runReaper(final Properties params) throws Exception {
        final long executionId = operator.start(REAPER_JOB, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(executionId);
        jobExecution.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus(),
                "the reaper should complete normally");
        return jobExecution.getExitStatus();
    }

    /**
     * A reaper pointed at its own job finds its own execution in the page. With its explicit
     * self-check turned off, the operator's local-liveness screen is the only thing standing
     * between it and failing itself — and it holds.
     */
    @Test
    public void reaperDoesNotFailItselfEvenWithoutItsOwnSelfCheck() throws Exception {
        final Properties params = new Properties();
        params.setProperty("targetJobName", REAPER_JOB);
        params.setProperty("staleMinutes", "0");
        params.setProperty("selfProtect", "false");

        assertEquals("FAILED=0;SKIPPED=1;REMAINING=1", runReaper(params));
    }

    /** With its own self-check on, the reaper never even asks about its own execution. */
    @Test
    public void reaperDropsItsOwnExecutionId() throws Exception {
        final Properties params = new Properties();
        params.setProperty("targetJobName", REAPER_JOB);
        params.setProperty("staleMinutes", "0");
        params.setProperty("selfProtect", "true");

        assertEquals("FAILED=0;SKIPPED=0;REMAINING=1", runReaper(params));
    }

    /** Nothing stale is a normal outcome, not an error. */
    @Test
    public void reaperReportsWhenThereIsNothingToDo() throws Exception {
        startParkedExecution();     //makes the target job known to the repository
        final Properties params = new Properties();
        params.setProperty("targetJobName", JOB);
        params.setProperty("staleMinutes", "600");

        assertEquals(AdminOperationsBatchlet.NOTHING_STALE, runReaper(params));
    }
}
