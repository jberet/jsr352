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
import java.util.List;

import jakarta.batch.operations.BatchRuntimeException;

/**
 * Thrown by {@link ExtendedJobOperator#failJobExecutions(List, FailOptions)} when the single
 * transaction could not be committed.
 * <p>
 * When this is thrown <em>nothing was changed</em>: the transaction was rolled back and every
 * requested execution retains the batch status it had before the call. The ids that were attempted
 * are available from {@link #getRequestedExecutionIds()} so a caller can retry or report them.
 *
 * @since 2.2.0
 */
public class FailJobExecutionsException extends BatchRuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<Long> requestedExecutionIds;

    /**
     * Creates an exception carrying the ids that were attempted.
     *
     * @param requestedExecutionIds the execution ids passed to the repository; may be {@code null}
     * @param cause                 the underlying failure
     */
    public FailJobExecutionsException(final Collection<Long> requestedExecutionIds, final Throwable cause) {
        super(cause == null ? null : cause.getMessage(), cause);
        this.requestedExecutionIds = requestedExecutionIds == null ? Collections.<Long>emptyList()
                : Collections.unmodifiableList(new ArrayList<Long>(requestedExecutionIds));
    }

    /**
     * Returns the execution ids that were attempted and, because the transaction rolled back,
     * were not changed.
     *
     * @return an unmodifiable list of execution ids; never {@code null}
     */
    public List<Long> getRequestedExecutionIds() {
        return requestedExecutionIds;
    }
}
