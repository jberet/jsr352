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

import java.util.List;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.NoSuchJobException;

/**
 * An extension to {@link JobOperator} for managing stale and stuck job executions.
 * <p>
 * This interface is implemented by {@link AbstractJobOperator} and therefore by every job operator
 * JBeret produces, in Java SE, Jakarta EE and any other container that builds on JBeret.
 * <p>
 * Callers that may run against a non-JBeret {@code JobOperator} should test with {@code instanceof}:
 * <pre>
 * JobOperator op = BatchRuntime.getJobOperator();
 * if (op instanceof ExtendedJobOperator) {
 *     ((ExtendedJobOperator) op).failJobExecutions(ids);
 * }
 * </pre>
 * Whether these methods are supported also depends on the configured
 * {@link org.jberet.repository.JobRepository}; the repositories that cannot implement the moethods
 * throw {@link UnsupportedOperationException}.
 *
 * @since 3.2.1
 */
public interface ExtendedJobOperator extends JobOperator {

    /** Default exit status applied to executions failed administratively. */
    String FAILED_BY_ADMIN = "FAILED_BY_ADMIN";

    /**
     * Returns a page of job execution ids for the named job whose batch status is
     * {@code STARTED} or {@code STOPPING} and whose last-updated time is strictly older than
     * {@code minutes} minutes before now.
     * <p>
     * The cut-off is computed from the clock of the JVM making the call, because that is the clock
     * JBeret uses when writing {@code lastUpdatedTime}. In a cluster sharing one job repository,
     * the accuracy of this operation is bounded by clock skew between nodes.
     * <p>
     * Executions whose last-updated time was never recorded fall back to their start time, then
     * their create time, so an execution that died before its first progress update is still found.
     *
     * @param jobName the job name; must not be {@code null}
     * @param minutes minutes of inactivity; must be {@code >= 0}. {@code 0} selects every
     *                {@code STARTED}/{@code STOPPING} execution
     * @param start   zero-based offset of the first result; must be {@code >= 0}
     * @param count   maximum number of ids to return; must be {@code > 0}
     *
     * @return the page; never {@code null}, possibly empty
     *
     * @throws NoSuchJobException            if no job of that name is known to the repository
     * @throws JobSecurityException          if the caller is not permitted to read the job
     * @throws IllegalArgumentException      if an argument is out of range
     * @throws UnsupportedOperationException if the configured job repository cannot support it
     */
    StaleExecutionsPage listExecutionsNotUpdatedInMinutes(String jobName, int minutes,
                                                          int start, int count)
            throws NoSuchJobException, JobSecurityException;

    /**
     * Transitions the given job executions to {@code FAILED}, together with their non-terminal
     * step and partition executions, in a single transaction.
     * <p>
     * Equivalent to {@link #failJobExecutions(List, FailOptions)} with
     * {@link FailOptions#defaults()}.
     *
     * @param executionIds execution ids to fail; must not be {@code null}
     *
     * @return a result describing what was failed and what was skipped; never {@code null}
     *
     * @throws JobSecurityException          if the caller is not authorised
     * @throws FailJobExecutionsException    if the transaction could not be committed
     * @throws IllegalArgumentException      if {@code executionIds} is null or over the cap
     * @throws UnsupportedOperationException if the configured job repository cannot support it
     */
    FailExecutionsResult failJobExecutions(List<Long> executionIds) throws JobSecurityException;

    /**
     * Transitions the given job executions to {@code FAILED} in a single transaction.
     *
     * <h4>What is changed</h4>
     * For each accepted execution:
     * <ul>
     *   <li>partition executions in {@code STARTING}, {@code STARTED} or {@code STOPPING} whose
     *       step belongs to the execution &rarr; {@code FAILED}, exit status set;</li>
     *   <li>step executions in {@code STARTING}, {@code STARTED} or {@code STOPPING} &rarr;
     *       {@code FAILED}, exit status set, end time set;</li>
     *   <li>the job execution &rarr; {@code FAILED}, exit status set, end time set,
     *       last-updated time set.</li>
     * </ul>
     * {@code COMPLETED}, {@code FAILED}, {@code STOPPED} and {@code ABANDONED} steps and partitions
     * are left exactly as they are.
     *
     * <h4>What is skipped</h4>
     * An execution is skipped, not failed, and does not abort the operation, when it does not
     * exist, does not belong to the operator's scope, is not in {@code STARTED} or {@code STOPPING},
     * is detectably running in this JVM, or changed status between the read and the write. Each
     * skip is reported in {@link FailExecutionsResult#getSkipped()} with a reason and logged at
     * WARN. Asking to fail an already-{@code FAILED} execution is a skip, which makes the operation
     * idempotent.
     *
     * <h4>Atomicity</h4>
     * All accepted executions are failed in one transaction. If the commit fails, nothing is
     * changed, the failure is logged with every requested execution id and the exception detail,
     * and {@link FailJobExecutionsException} is thrown. There are no nested transactions and no
     * per-execution commits.
     *
     * <h4>Cluster limitation</h4>
     * Liveness is only detectable within the calling JVM. An execution that is genuinely running on
     * another node sharing the same job repository will not be screened out, and that node's
     * runtime may later write over the administrative change. Choose {@code minutes} comfortably
     * larger than the longest checkpoint interval, and prefer to run this after the suspect node is
     * confirmed down.
     *
     * @param executionIds execution ids to fail; must not be {@code null}. Duplicates are collapsed
     *                     and the size must not exceed {@link FailOptions#getMaxBatchSize()}
     * @param options      tuning for exit status, local-liveness screening and batch size;
     *                     {@code null} means {@link FailOptions#defaults()}
     *
     * @return a result describing what was failed and what was skipped; never {@code null}
     *
     * @throws JobSecurityException          if the caller is not authorised
     * @throws FailJobExecutionsException    if the transaction could not be committed
     * @throws IllegalArgumentException      if {@code executionIds} is null or over the cap
     * @throws UnsupportedOperationException if the configured job repository cannot support it
     */
    FailExecutionsResult failJobExecutions(List<Long> executionIds, FailOptions options)
            throws JobSecurityException;
}
