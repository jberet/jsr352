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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

/**
 * Tests the defaults, the derivation methods and the immutability of {@link FailOptions}.
 */
public class FailOptionsTest {

    @Test
    public void defaultsAreTheDocumentedOnes() {
        final FailOptions defaults = FailOptions.defaults();

        assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, defaults.getExitStatus());
        assertTrue(defaults.isSkipLocallyRunning());
        assertEquals(FailOptions.DEFAULT_MAX_BATCH_SIZE, defaults.getMaxBatchSize());
        assertEquals(1000, defaults.getMaxBatchSize());
        assertNull(defaults.getUser());
    }

    @Test
    public void defaultsIsShared() {
        assertSame(FailOptions.defaults(), FailOptions.defaults());
    }

    /** Every {@code with*} method returns a new instance and leaves the receiver untouched. */
    @Test
    public void derivationDoesNotMutateTheReceiver() {
        final FailOptions defaults = FailOptions.defaults();

        final FailOptions derived = defaults
                .withExitStatus("CUSTOM")
                .withSkipLocallyRunning(false)
                .withMaxBatchSize(7)
                .withUser("alice");

        assertNotSame(defaults, derived);
        assertEquals("CUSTOM", derived.getExitStatus());
        assertFalse(derived.isSkipLocallyRunning());
        assertEquals(7, derived.getMaxBatchSize());
        assertEquals("alice", derived.getUser());

        assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, defaults.getExitStatus());
        assertTrue(defaults.isSkipLocallyRunning());
        assertEquals(FailOptions.DEFAULT_MAX_BATCH_SIZE, defaults.getMaxBatchSize());
        assertNull(defaults.getUser());
    }

    /** Each derivation carries forward every other setting. */
    @Test
    public void derivationPreservesUnrelatedSettings() {
        final FailOptions base = FailOptions.defaults().withUser("bob").withMaxBatchSize(5);

        final FailOptions derived = base.withExitStatus("X");

        assertEquals("bob", derived.getUser());
        assertEquals(5, derived.getMaxBatchSize());
        assertTrue(derived.isSkipLocallyRunning());
    }

    @Test
    public void nullExitStatusIsRejected() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> FailOptions.defaults().withExitStatus(null));
        assertTrue(e.getMessage().contains("exitStatus"), e.getMessage());
    }

    @Test
    public void nonPositiveMaxBatchSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FailOptions.defaults().withMaxBatchSize(0));
        assertThrows(IllegalArgumentException.class, () -> FailOptions.defaults().withMaxBatchSize(-1));
    }

    /** A null user is allowed: it simply means "no user recorded in the audit line". */
    @Test
    public void nullUserIsAllowed() {
        assertNull(FailOptions.defaults().withUser("x").withUser(null).getUser());
    }

    /** The type crosses a remote boundary, so it has to round-trip through serialization. */
    @Test
    public void isSerializable() throws Exception {
        final FailOptions original = FailOptions.defaults()
                .withExitStatus("CUSTOM").withSkipLocallyRunning(false).withMaxBatchSize(3).withUser("carol");

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        final FailOptions restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (FailOptions) in.readObject();
        }

        assertEquals(original.getExitStatus(), restored.getExitStatus());
        assertEquals(original.isSkipLocallyRunning(), restored.isSkipLocallyRunning());
        assertEquals(original.getMaxBatchSize(), restored.getMaxBatchSize());
        assertEquals(original.getUser(), restored.getUser());
    }
}
