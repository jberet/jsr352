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

import java.util.Date;
import java.util.stream.Stream;

import jakarta.batch.runtime.BatchStatus;
import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the checkpoint behaviour of {@link InfinispanRepository#savePersistentDataIfNotStopping}.
 *
 * <p>Uses embedded, local (no {@code <transport/>}) Infinispan caches.
 * Every test runs twice: once against non-transactional caches, where the step execution and job
 * execution writes are applied sequentially, and once against transactional caches, where
 * {@code InfinispanRepository} wraps both writes in a single JTA transaction.</p>
 *
 * @see InfinispanRepository#savePersistentDataIfNotStopping
 */
public class InfinispanRepositoryCheckpointTest {

    private static InfinispanRepository nonTransactionalRepository;
    private static InfinispanRepository transactionalRepository;
    private static Job job;

    @BeforeAll
    static void setUp() {
        nonTransactionalRepository = new InfinispanRepository("local-infinispan.xml");
        transactionalRepository = new InfinispanRepository("local-transactional-infinispan.xml");
        job = new Job("infinispan-checkpoint-test-job");
    }

    @AfterAll
    static void tearDown() {
        for (final InfinispanRepository repository : new InfinispanRepository[]{
                nonTransactionalRepository, transactionalRepository}) {
            if (repository != null) {
                repository.stopCacheManager();
            }
        }
    }

    static Stream<Arguments> repositories() {
        return Stream.of(
                Arguments.of("non-transactional caches", nonTransactionalRepository),
                Arguments.of("transactional caches", transactionalRepository));
    }

    /**
     * Guards the premise of the parameterized tests below: checkpoint writes in a JTA transaction and one non 
     * JTA transaction.
     */
    @Test
    void theTwoTestRepositoriesDifferInTransactionality() {
        assertNull(nonTransactionalRepository.getTransactionManager(),
                "local-infinispan.xml must configure non-transactional caches");
        assertNotNull(transactionalRepository.getTransactionManager(),
                "local-transactional-infinispan.xml must give the step and job execution caches a shared " +
                        "transaction manager, otherwise the transactional test cases prove nothing");
    }

    /**
     * Verifies that a checkpoint on a running step refreshes the enclosing job execution's
     * {@code lastUpdatedTime} in the {@code JOB_EXECUTION} cache, and that it does so again on every subsequent
     * checkpoint rather than only when the job ends.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("repositories")
    void updatesJobExecutionLastUpdatedTimeOnEveryCheckpoint(final String name,
                                                             final InfinispanRepository repository)
            throws Exception {
        final JobExecutionImpl jobExecution = startJobExecution(repository);
        final StepExecutionImpl stepExecution = startStepExecution(repository, jobExecution);

        Date previous = null;
        for (int checkpoint = 1; checkpoint <= 3; checkpoint++) {
            final long beforeCheckpoint = System.currentTimeMillis() - 1L;

            final int savedCount = repository.savePersistentDataIfNotStopping(jobExecution, stepExecution);
            assertEquals(1, savedCount,
                    "savePersistentDataIfNotStopping should report 1 row saved for a STARTED step");

            final Date lastUpdated = lastUpdatedTimeOf(repository, jobExecution);
            assertNotNull(lastUpdated, "lastUpdatedTime must not be null after checkpoint " + checkpoint);
            assertTrue(lastUpdated.getTime() >= beforeCheckpoint,
                    "lastUpdatedTime (" + lastUpdated.getTime() + ") after checkpoint " + checkpoint +
                            " must be >= the time before that checkpoint (" + beforeCheckpoint + ")");
            if (previous != null) {
                assertTrue(lastUpdated.getTime() >= previous.getTime(),
                        "lastUpdatedTime must never go backwards between checkpoints");
            }
            previous = lastUpdated;

            //let the millisecond clock advance so that successive checkpoints are distinguishable
            Thread.sleep(5L);
        }
    }

    /**
     * Verifies that the step execution checkpoint is written to the {@code STEP_EXECUTION} cache, and not
     * only the job execution's {@code lastUpdatedTime}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("repositories")
    void savesStepExecutionCheckpointData(final String name, final InfinispanRepository repository) {
        final JobExecutionImpl jobExecution = startJobExecution(repository);
        final StepExecutionImpl stepExecution = startStepExecution(repository, jobExecution);
        stepExecution.setPersistentUserData("checkpoint-data");

        assertEquals(1, repository.savePersistentDataIfNotStopping(jobExecution, stepExecution));

        final StepExecutionImpl saved = savedStepExecution(repository, stepExecution);
        assertNotNull(saved, "the step execution must be present in the STEP_EXECUTION cache");
        assertEquals("checkpoint-data", saved.getPersistentUserData(),
                "the checkpoint's persistent user data must have been written to the cache");
    }

    /**
     * Verifies that a step execution already marked {@code STOPPING} is left alone.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("repositories")
    void doesNotSaveWhenStepExecutionIsStopping(final String name, final InfinispanRepository repository)
            throws Exception {
        final JobExecutionImpl jobExecution = startJobExecution(repository);
        final StepExecutionImpl stepExecution = startStepExecution(repository, jobExecution);

        //an initial checkpoint, so that lastUpdatedTime has a known value to compare against
        assertEquals(1, repository.savePersistentDataIfNotStopping(jobExecution, stepExecution));
        final Date beforeStopping = lastUpdatedTimeOf(repository, jobExecution);
        assertNotNull(beforeStopping);

        //JobOperator.stop() marks the saved step execution STOPPING while the step is still running
        stepExecution.setBatchStatus(BatchStatus.STOPPING);
        repository.updateStepExecution(stepExecution);
        stepExecution.setPersistentUserData("data-that-must-not-be-saved");

        Thread.sleep(5L);
        final int savedCount = repository.savePersistentDataIfNotStopping(jobExecution, stepExecution);

        assertEquals(0, savedCount,
                "savePersistentDataIfNotStopping should report 0 rows saved for a STOPPING step");
        assertEquals(beforeStopping, lastUpdatedTimeOf(repository, jobExecution),
                "lastUpdatedTime must not be refreshed when the step execution is STOPPING");
        assertNull(savedStepExecution(repository, stepExecution).getPersistentUserData(),
                "no checkpoint data must be written when the step execution is STOPPING");
    }

    private static JobExecutionImpl startJobExecution(final InfinispanRepository repository) {
        final JobInstanceImpl jobInstance = repository.createJobInstance(
                job, null, InfinispanRepositoryCheckpointTest.class.getClassLoader());
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, null);
        jobExecution.setBatchStatus(BatchStatus.STARTED);
        repository.updateJobExecution(jobExecution, false, false);
        return jobExecution;
    }

    private static StepExecutionImpl startStepExecution(final InfinispanRepository repository,
                                                        final JobExecutionImpl jobExecution) {
        final StepExecutionImpl stepExecution = new StepExecutionImpl("step1");
        stepExecution.setBatchStatus(BatchStatus.STARTED);
        repository.addStepExecution(jobExecution, stepExecution);
        return stepExecution;
    }

    /**
     * Reads the job execution back out of the {@code JOB_EXECUTION} cache so that the assertion is made against
     * the stored copy rather than against the in-memory instance the test still holds.
     */
    private static Date lastUpdatedTimeOf(final InfinispanRepository repository,
                                          final JobExecutionImpl jobExecution) {
        final JobExecutionImpl stored = repository.getJobExecution(jobExecution.getExecutionId());
        assertNotNull(stored, "the job execution must be present in the JOB_EXECUTION cache");
        return stored.getLastUpdatedTime();
    }

    /**
     * Reads the step execution back out of the {@code STEP_EXECUTION} cache.
     */
    private static StepExecutionImpl savedStepExecution(final InfinispanRepository repository,
                                                        final StepExecutionImpl stepExecution) {
        return repository.getStepExecutionCache().get(stepExecution.getStepExecutionId());
    }
}
