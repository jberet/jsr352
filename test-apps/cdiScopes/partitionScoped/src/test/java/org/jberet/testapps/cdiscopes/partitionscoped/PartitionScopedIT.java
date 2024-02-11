/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.partitionscoped;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link org.jberet.cdi.PartitionScoped}.
 */
public class PartitionScopedIT extends AbstractIT {
    static final String partitionScopedTest = "partitionScopedPartitioned";
    static final String partitionScopedFailJobListenerTest = "partitionScopedFailJobListener";
    static final String partitionScopedFailStepListenerTest = "partitionScopedFailStepListener";

    @Test
    public void partitionScopedTest() throws Exception {
        //same partition, different artifacts, injected Foo should be different
        //different partition, injected Foo should be the different

        startJobAndWait(partitionScopedTest);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final String exitStatus = stepExecution0.getExitStatus();
        System.out.printf("step exit status: %s%n", exitStatus);
        // There's not guarantee which orders threads will be processed in, just check the existStatus contains
        // each value from the expected data.
        for (String expected : PartitionScopePartitionAnalyzer.expectedData) {
            Assertions.assertTrue(exitStatus.contains(expected), "Missing expected data '" + expected + "' in '" + exitStatus + "'");
        }
    }

    @Test
    public void partitionScopedFailJobListener() throws Exception {
        //injecting @PartitionScoped Foo into a job listener will fail
        startJobAndWait(partitionScopedFailJobListenerTest);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void partitionScopedFailStepListener() throws Exception {
        //injecting @PartitionScoped Foo into a step listener will fail
        startJobAndWait(partitionScopedFailStepListenerTest);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}