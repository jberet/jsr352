/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.cdiscopes.partitionscoped;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link org.jberet.cdi.StepScoped}.
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
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final String exitStatus = stepExecution0.getExitStatus();
        System.out.printf("step exit status: %s%n", exitStatus);
        Assert.assertEquals(PartitionScopePartitionAnalyzer.expectedData.toString(), exitStatus);
    }

    @Test
    public void partitionScopedFailJobListener() throws Exception {
        //injecting @PartitionScoped Foo into a job listener will fail
        startJobAndWait(partitionScopedFailJobListenerTest);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void partitionScopedFailStepListener() throws Exception {
        //injecting @PartitionScoped Foo into a step listener will fail
        startJobAndWait(partitionScopedFailStepListenerTest);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}