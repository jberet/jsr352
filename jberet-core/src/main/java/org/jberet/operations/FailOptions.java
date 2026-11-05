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

import java.io.Serializable;
import java.util.List;

import org.jberet._private.BatchMessages;

/**
 * Immutable tuning for {@link ExtendedJobOperator#failJobExecutions(List, FailOptions)}.
 * <p>
 * Obtain a starting point with {@link #defaults()} and derive variants with the {@code with*}
 * methods, each of which returns a new instance:
 * <pre>
 * FailOptions opts = FailOptions.defaults()
 *         .withExitStatus("FAILED_BY_REAPER")
 *         .withMaxBatchSize(100);
 * </pre>
 *
 * @since 2.2.0
 */
public final class FailOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Default upper bound on the number of executions failed in one transaction. */
    public static final int DEFAULT_MAX_BATCH_SIZE = 1000;

    private static final FailOptions DEFAULTS =
            new FailOptions(ExtendedJobOperator.FAILED_BY_ADMIN, true, DEFAULT_MAX_BATCH_SIZE, null);

    private final String exitStatus;
    private final boolean skipLocallyRunning;
    private final int maxBatchSize;
    private final String user;

    private FailOptions(final String exitStatus, final boolean skipLocallyRunning,
                        final int maxBatchSize, final String user) {
        this.exitStatus = exitStatus;
        this.skipLocallyRunning = skipLocallyRunning;
        this.maxBatchSize = maxBatchSize;
        this.user = user;
    }

    /**
     * Returns the default options: exit status {@link ExtendedJobOperator#FAILED_BY_ADMIN},
     * local-liveness screening enabled, a batch cap of {@value #DEFAULT_MAX_BATCH_SIZE}, and no user.
     *
     * @return the default options; never {@code null}
     */
    public static FailOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Derives options with a different exit status.
     *
     * @param exitStatus the exit status to record on everything changed; must not be {@code null}
     *
     * @return a new instance
     *
     * @throws IllegalArgumentException if {@code exitStatus} is {@code null}
     */
    public FailOptions withExitStatus(final String exitStatus) {
        if (exitStatus == null) {
            throw BatchMessages.MESSAGES.nullVar("exitStatus");
        }
        return new FailOptions(exitStatus, skipLocallyRunning, maxBatchSize, user);
    }

    /**
     * Derives options that do or do not screen out executions still running in this JVM.
     * <p>
     * Screening is on by default and is what stops a reaper batchlet from failing its own execution.
     * Turning it off is only sensible when the caller knows the runtime for those executions is gone.
     *
     * @param skipLocallyRunning whether to skip executions detectably alive in this JVM
     *
     * @return a new instance
     */
    public FailOptions withSkipLocallyRunning(final boolean skipLocallyRunning) {
        return new FailOptions(exitStatus, skipLocallyRunning, maxBatchSize, user);
    }

    /**
     * Derives options with a different cap on how many executions one call may fail.
     *
     * @param maxBatchSize the cap; must be {@code > 0}
     *
     * @return a new instance
     *
     * @throws IllegalArgumentException if {@code maxBatchSize <= 0}
     */
    public FailOptions withMaxBatchSize(final int maxBatchSize) {
        if (maxBatchSize <= 0) {
            throw BatchMessages.MESSAGES.invalidMaxBatchSize(maxBatchSize);
        }
        return new FailOptions(exitStatus, skipLocallyRunning, maxBatchSize, user);
    }

    /**
     * Derives options recording the user on whose behalf the operation is performed. The user is
     * written to the audit log line only; it is not persisted on the job execution.
     *
     * @param user the user name, may be {@code null}
     *
     * @return a new instance
     */
    public FailOptions withUser(final String user) {
        return new FailOptions(exitStatus, skipLocallyRunning, maxBatchSize, user);
    }

    /**
     * @return the exit status recorded on every job, step and partition execution changed
     */
    public String getExitStatus() {
        return exitStatus;
    }

    /**
     * @return whether executions detectably running in this JVM are skipped
     */
    public boolean isSkipLocallyRunning() {
        return skipLocallyRunning;
    }

    /**
     * @return the maximum number of distinct execution ids one call may fail
     */
    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    /**
     * @return the user recorded in the audit log line, or {@code null}
     */
    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "FailOptions{exitStatus=" + exitStatus
                + ", skipLocallyRunning=" + skipLocallyRunning
                + ", maxBatchSize=" + maxBatchSize
                + ", user=" + user + '}';
    }
}
