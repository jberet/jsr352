/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.stepscoped;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link org.jberet.cdi.StepScoped}.
 */
public class StepScopedIT extends AbstractIT {
    static final String stepScopedTest = "stepScoped";
    static final String stepScopedFailedTest = "stepScopedFail";
    static final String stepScopedTest2 = "stepScoped2";
    static final String stepScopedPartitionedTest = "stepScopedPartitioned";

    @Test
    public void stepScopedTest() throws Exception {
        final String stepName1Repeat3 =
"stepScoped.step1TYPE stepScoped.step1TYPE stepScoped.step1TYPE stepScoped.step1METHOD stepScoped.step1METHOD stepScoped.step1METHOD stepScoped.step1FIELD stepScoped.step1FIELD stepScoped.step1FIELD";

        final String stepName2 = "stepScoped.step2TYPE stepScoped.step2METHOD stepScoped.step2FIELD";

        //same job, different steps, injected Foo should be different
        //same step, different artifact, injected Foo (into both batchlet and step listener) should be the same

        startJobAndWait(stepScopedTest);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(stepName1Repeat3, stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(stepName2, stepExecutions.get(1).getExitStatus());

        stepExecutions.clear();
        jobExecution = null;
        stepExecutions = null;

        final String job2StepName1 = "stepScoped2.step1TYPE stepScoped2.step1METHOD stepScoped2.step1FIELD";
        final String job2StepName2 = "stepScoped2.step2TYPE stepScoped2.step2METHOD stepScoped2.step2FIELD";

        //run a different job (stepScoped2) to check that a different Foo instance is used within the scope of stepScoped2
        startJobAndWait(stepScopedTest2);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(job2StepName1, stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(job2StepName2, stepExecutions.get(1).getExitStatus());
    }

    @Test
    public void stepScopedPartitionedTest() throws Exception {
        startJobAndWait(stepScopedPartitionedTest);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        //step2 exit status is set in analyzer, to foo.stepNames.
        final String stepExitStatus = stepExecution0.getExitStatus();

        //beforeStep, afterStep, batchlet * 3 partitioins = 5
        //and there are 3 injections: Foo, FooMethodTarget & FooFieldTarget
        Assert.assertEquals(
"stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD",
                stepExitStatus);
    }

    @Test
    public void stepScopedFail() throws Exception {
        //injecting @StepScoped Foo into a job listener will fail
        startJobAndWait(stepScopedFailedTest);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}