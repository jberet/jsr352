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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.batch.runtime.BatchStatus;
import org.jberet._private.BatchMessages;

/**
 * The outcome of {@link ExtendedJobOperator#failJobExecutions(List, FailOptions)}: which executions
 * were transitioned to {@link BatchStatus#FAILED} and which were skipped, with a reason for each
 * skip.
 * <p>
 *
 * @since 3.2.1
 */
public final class FailExecutionsResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final FailExecutionsResult EMPTY =
            new FailExecutionsResult(Collections.<Long>emptyList(),
                    Collections.<SkippedExecution>emptyList());

    /**
     * Why an execution that was asked for was not transitioned to {@code FAILED}.
     */
    public enum SkipReason {
        NOT_FOUND,
        NOT_IN_SCOPE,
        /** The execution is not in {@code STARTED} or {@code STOPPING}. */
        INVALID_BATCH_STATUS,
        RUNNING_LOCALLY,
        /** The execution changed status between the read and the guarded update. */
        CONCURRENTLY_MODIFIED
    }

    /**
     * One skipped execution and the reason it was skipped.
     */
    public static final class SkippedExecution implements Serializable {

        private static final long serialVersionUID = 1L;

        private final long executionId;
        private final SkipReason reason;
        private final BatchStatus observedStatus;
        private final String message;

        SkippedExecution(final long executionId, final SkipReason reason,
                         final BatchStatus observedStatus, final String message) {
            this.executionId = executionId;
            this.reason = reason;
            this.observedStatus = observedStatus;
            this.message = message;
        }

        /**
         * Creates a skip for an execution id that does not exist.
         *
         * @param executionId the execution id
         *
         * @return the skipped execution
         */
        public static SkippedExecution notFound(final long executionId) {
            return new SkippedExecution(executionId, SkipReason.NOT_FOUND, null,
                    BatchMessages.MESSAGES.skipReasonNotFound(executionId));
        }

        /**
         * Creates a skip for an execution belonging to a different job than the operator is scoped to.
         *
         * @param executionId the execution id
         *
         * @return the skipped execution
         */
        public static SkippedExecution notInScope(final long executionId) {
            return new SkippedExecution(executionId, SkipReason.NOT_IN_SCOPE, null,
                    BatchMessages.MESSAGES.skipReasonNotInScope(executionId));
        }

        /**
         * Creates a skip for an execution that is not in {@code STARTED} or {@code STOPPING}.
         *
         * @param executionId the execution id
         * @param observed    the batch status that was observed
         *
         * @return the skipped execution
         */
        public static SkippedExecution invalidStatus(final long executionId, final BatchStatus observed) {
            return new SkippedExecution(executionId, SkipReason.INVALID_BATCH_STATUS, observed,
                    BatchMessages.MESSAGES.skipReasonInvalidBatchStatus(executionId, observed));
        }

        /**
         * Creates a skip for an execution that is still running in this JVM.
         *
         * @param executionId the execution id
         *
         * @return the skipped execution
         */
        public static SkippedExecution runningLocally(final long executionId) {
            return new SkippedExecution(executionId, SkipReason.RUNNING_LOCALLY, null,
                    BatchMessages.MESSAGES.skipReasonRunningLocally(executionId));
        }

        /**
         * Creates a skip for an execution whose status changed between the read and the write.
         *
         * @param executionId the execution id
         * @param observed    the batch status observed before the attempt, may be {@code null}
         *
         * @return the skipped execution
         */
        public static SkippedExecution concurrentlyModified(final long executionId, final BatchStatus observed) {
            return new SkippedExecution(executionId, SkipReason.CONCURRENTLY_MODIFIED, observed,
                    BatchMessages.MESSAGES.skipReasonConcurrentlyModified(executionId));
        }

        /**
         * @return the execution id that was skipped
         */
        public long getExecutionId() {
            return executionId;
        }

        /**
         * @return why the execution was skipped; never {@code null}
         */
        public SkipReason getReason() {
            return reason;
        }

        /**
         * @return the batch status observed before the attempt, or {@code null} when unknown
         */
        public BatchStatus getObservedStatus() {
            return observedStatus;
        }

        /**
         * @return a human-readable, localised description of the skip
         */
        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    private final List<Long> failedExecutionIds;
    private final List<SkippedExecution> skipped;

    /**
     * Creates a result.
     *
     * @param failedExecutionIds ids transitioned to {@code FAILED}; may be {@code null}
     * @param skipped            executions that were skipped; may be {@code null}
     */
    public FailExecutionsResult(final List<Long> failedExecutionIds, final List<SkippedExecution> skipped) {
        this.failedExecutionIds = failedExecutionIds == null ? Collections.<Long>emptyList()
                : Collections.unmodifiableList(new ArrayList<Long>(failedExecutionIds));
        this.skipped = skipped == null ? Collections.<SkippedExecution>emptyList()
                : Collections.unmodifiableList(new ArrayList<SkippedExecution>(skipped));
    }

    /**
     * @return a result with nothing failed and nothing skipped
     */
    public static FailExecutionsResult empty() {
        return EMPTY;
    }

    /**
     * @return the number of executions transitioned to {@code FAILED}
     */
    public int getFailedCount() {
        return failedExecutionIds.size();
    }

    /**
     * @return an unmodifiable list of the ids transitioned to {@code FAILED}; never {@code null}
     */
    public List<Long> getFailedExecutionIds() {
        return failedExecutionIds;
    }

    /**
     * @return the number of executions that were skipped
     */
    public int getSkippedCount() {
        return skipped.size();
    }

    /**
     * @return an unmodifiable list describing every skipped execution; never {@code null}
     */
    public List<SkippedExecution> getSkipped() {
        return skipped;
    }

    @Override
    public String toString() {
        return "FailExecutionsResult{failed=" + failedExecutionIds + ", skipped=" + skipped + '}';
    }
}
