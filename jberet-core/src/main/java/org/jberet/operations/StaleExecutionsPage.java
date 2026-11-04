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

/**
 * An immutable page of job execution ids returned by
 * {@link ExtendedJobOperator#listExecutionsNotUpdatedInMinutes(String, int, int, int)}.
 * <p>
 *
 * @see ExtendedJobOperator#listExecutionsNotUpdatedInMinutes(String, int, int, int)
 * @since 3.2.1
 */
public final class StaleExecutionsPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Long> executionIds;
    private final long totalCount;
    private final int start;
    private final int count;

    /**
     * Creates a page.
     *
     * @param executionIds the execution ids in this page, in ascending order; may be {@code null},
     *                     which is treated as an empty page
     * @param totalCount   the total number of matching executions at the moment the count was taken
     * @param start        the zero-based offset this page starts at
     * @param count        the number of ids in this page; must equal {@code executionIds.size()}
     *
     * @throws IllegalArgumentException if {@code count} does not match the number of ids
     */
    public StaleExecutionsPage(final List<Long> executionIds, final long totalCount,
                               final int start, final int count) {
        final List<Long> ids = executionIds == null ? Collections.<Long>emptyList()
                : Collections.unmodifiableList(new ArrayList<Long>(executionIds));
        if (ids.size() != count) {
            throw new IllegalArgumentException("count " + count + " != executionIds.size() " + ids.size());
        }
        this.executionIds = ids;
        this.totalCount = totalCount;
        this.start = start;
        this.count = count;
    }

    /**
     * Returns an empty page at the given offset.
     *
     * @param start      the zero-based offset
     * @param totalCount the total number of matching executions
     *
     * @return an empty page; never {@code null}
     */
    public static StaleExecutionsPage empty(final int start, final long totalCount) {
        return new StaleExecutionsPage(Collections.<Long>emptyList(), totalCount, start, 0);
    }

    /**
     * Returns the execution ids in this page, ordered by ascending execution id.
     *
     * @return an unmodifiable list; never {@code null}, possibly empty
     */
    public List<Long> getExecutionIds() {
        return executionIds;
    }

    /**
     * Total number of matching executions at the moment the count query ran. Read with a separate
     * statement from the page itself, so under concurrent change it may not equal the number of ids
     * a full walk would yield. Treat it as a progress indicator, not an invariant.
     *
     * @return the total number of matching executions
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * Returns the zero-based offset of the first id in this page.
     *
     * @return the offset this page starts at
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the number of ids actually in this page, which may be fewer than the requested count.
     *
     * @return the size of this page
     */
    public int getCount() {
        return count;
    }

    /**
     * Indicates whether more matching executions existed beyond this page when the count was taken.
     *
     * @return {@code true} if {@code start + count < totalCount}
     */
    public boolean hasMore() {
        return (long) start + (long) count < totalCount;
    }

    @Override
    public String toString() {
        return "StaleExecutionsPage{executionIds=" + executionIds
                + ", totalCount=" + totalCount
                + ", start=" + start
                + ", count=" + count + '}';
    }
}
