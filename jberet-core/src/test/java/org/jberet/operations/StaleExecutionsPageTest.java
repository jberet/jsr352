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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the immutability and the {@code hasMore()} arithmetic of {@link StaleExecutionsPage}.
 */
public class StaleExecutionsPageTest {

    @Test
    public void accessorsReturnWhatWasConstructed() {
        final StaleExecutionsPage page = new StaleExecutionsPage(Arrays.asList(3L, 5L, 9L), 42L, 10, 3);

        assertEquals(Arrays.asList(3L, 5L, 9L), page.getExecutionIds());
        assertEquals(42L, page.getTotalCount());
        assertEquals(10, page.getStart());
        assertEquals(3, page.getCount());
    }

    /** The page copies its input, so a later change to the caller's list cannot reach it. */
    @Test
    public void pageIsDecoupledFromTheSourceList() {
        final List<Long> source = new ArrayList<Long>(Arrays.asList(1L, 2L));
        final StaleExecutionsPage page = new StaleExecutionsPage(source, 2L, 0, 2);

        source.add(3L);

        assertEquals(2, page.getExecutionIds().size());
    }

    @Test
    public void executionIdsAreUnmodifiable() {
        final StaleExecutionsPage page = new StaleExecutionsPage(Arrays.asList(1L), 1L, 0, 1);

        assertThrows(UnsupportedOperationException.class, () -> page.getExecutionIds().add(2L));
    }

    @Test
    public void nullIdsMeanAnEmptyPage() {
        final StaleExecutionsPage page = new StaleExecutionsPage(null, 0L, 0, 0);

        assertTrue(page.getExecutionIds().isEmpty());
        assertFalse(page.hasMore());
    }

    /** The count must describe the page it is attached to, or every caller's arithmetic is wrong. */
    @Test
    public void countMustMatchTheNumberOfIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new StaleExecutionsPage(Arrays.asList(1L, 2L), 2L, 0, 5));
    }

    @Test
    public void hasMoreIsTrueOnlyWhenRowsRemain() {
        //a full first page of a larger result set
        assertTrue(new StaleExecutionsPage(Arrays.asList(1L, 2L), 10L, 0, 2).hasMore());
        //the last page, landing exactly on the total
        assertFalse(new StaleExecutionsPage(Arrays.asList(9L, 10L), 10L, 8, 2).hasMore());
        //a page that did not fill, so there is nothing after it
        assertFalse(new StaleExecutionsPage(Arrays.asList(9L), 9L, 8, 1).hasMore());
        //an empty page past the end
        assertFalse(StaleExecutionsPage.empty(100, 3L).hasMore());
        //an empty page of an empty result set
        assertFalse(StaleExecutionsPage.empty(0, 0L).hasMore());
    }

    @Test
    public void hasMoreDoesNotOverflow() {
        final StaleExecutionsPage page =
                new StaleExecutionsPage(Collections.singletonList(1L), Long.MAX_VALUE, Integer.MAX_VALUE, 1);

        assertTrue(page.hasMore());
    }

    @Test
    public void emptyFactoryProducesAnEmptyPageThatKeepsTheTotal() {
        final StaleExecutionsPage page = StaleExecutionsPage.empty(20, 7L);

        assertTrue(page.getExecutionIds().isEmpty());
        assertEquals(0, page.getCount());
        assertEquals(20, page.getStart());
        assertEquals(7L, page.getTotalCount());
    }

    /** The type crosses a remote boundary, so it has to round-trip through serialization. */
    @Test
    public void isSerializable() throws Exception {
        final StaleExecutionsPage original = new StaleExecutionsPage(Arrays.asList(1L, 2L, 3L), 99L, 6, 3);

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        final StaleExecutionsPage restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (StaleExecutionsPage) in.readObject();
        }

        assertEquals(original.getExecutionIds(), restored.getExecutionIds());
        assertEquals(original.getTotalCount(), restored.getTotalCount());
        assertEquals(original.getStart(), restored.getStart());
        assertEquals(original.getCount(), restored.getCount());
        assertTrue(restored.hasMore());
    }
}
