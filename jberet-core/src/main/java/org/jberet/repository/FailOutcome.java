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

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import jakarta.batch.runtime.BatchStatus;

/**
 * What a job repository did, or declined to do, with one execution id passed to
 * {@link JobRepository#failJobExecutions(Collection, String, String, Date)}.
 * 
 * @since 3.2.1
 */
public final class FailOutcome {

    /**
     * The kinds of outcome a single execution id can have.
     */
    public enum Kind {
        /** The execution was transitioned to {@code FAILED}. */
        FAILED,
        /** No execution with that id exists. */
        NOT_FOUND,
        /** The execution belongs to a different job than the one the operation is scoped to. */
        NOT_IN_SCOPE,
        /** The execution was not in {@code STARTED} or {@code STOPPING} when it was read. */
        INVALID_STATUS,
        /** The execution was eligible when read but had moved on by the time of the guarded update. */
        CONCURRENTLY_MODIFIED
    }

    public static final FailOutcome FAILED = new FailOutcome(Kind.FAILED, null);
    public static final FailOutcome NOT_FOUND = new FailOutcome(Kind.NOT_FOUND, null);
    public static final FailOutcome NOT_IN_SCOPE = new FailOutcome(Kind.NOT_IN_SCOPE, null);
    private final Kind kind;
    private final BatchStatus observedStatus;

    private FailOutcome(final Kind kind, final BatchStatus observedStatus) {
        this.kind = kind;
        this.observedStatus = observedStatus;
    }

    /**
     * @return the outcome for an execution that was failed
     */
    public static FailOutcome failed() {
        return FAILED;
    }

    /**
     * @return the outcome for an execution id that does not exist
     */
    public static FailOutcome notFound() {
        return NOT_FOUND;
    }

    /**
     * @return the outcome for an execution belonging to another job
     */
    public static FailOutcome notInScope() {
        return NOT_IN_SCOPE;
    }

    /**
     * @param observed the batch status that was read
     *
     * @return the outcome for an execution in a status that cannot be failed
     */
    public static FailOutcome invalidStatus(final BatchStatus observed) {
        return new FailOutcome(Kind.INVALID_STATUS, observed);
    }

    /**
     * @param observed the batch status that was read before the write was attempted
     *
     * @return the outcome for an execution that changed underneath the operation
     */
    public static FailOutcome concurrentlyModified(final BatchStatus observed) {
        return new FailOutcome(Kind.CONCURRENTLY_MODIFIED, observed);
    }

    /**
     * @return the kind of outcome; never {@code null}
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @return the status observed before the attempt, or {@code null} when it is not known
     */
    public BatchStatus observedStatus() {
        return observedStatus;
    }

    /**
     * Convenience for reading an outcome map that may have no entry for an id.
     *
     * @param outcomes    the outcome map, may be {@code null}
     * @param executionId the execution id to look up
     *
     * @return the recorded outcome, or {@link #NOT_FOUND} when there is none
     */
    public static FailOutcome of(final Map<Long, FailOutcome> outcomes, final Long executionId) {
        if (outcomes == null) {
            return NOT_FOUND;
        }
        final FailOutcome outcome = outcomes.get(executionId);
        return outcome == null ? NOT_FOUND : outcome;
    }

    @Override
    public String toString() {
        return observedStatus == null ? kind.name() : kind.name() + "(" + observedStatus + ')';
    }
}
