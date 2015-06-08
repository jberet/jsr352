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

package org.jberet.testapps.cdiscopes.stepscoped;

import java.util.Arrays;
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

    static final String step1 = "stepScoped.step1";
    static final String step2 = "stepScoped.step2";
    static final String stepName1Repeat3 = Arrays.asList((new String[]{step1, step1, step1})).toString();
    static final String stepName2 = Arrays.asList((new String[]{step2})).toString();

    static final String job2StepName1 = Arrays.asList((new String[]{"stepScoped2.step1"})).toString();
    static final String job2StepName2 = Arrays.asList((new String[]{"stepScoped2.step2"})).toString();


    @Test
    public void stepScopedTest() throws Exception {
        //same job, different steps, injected Foo should be different
        //same step, different artifact, injected Foo (into both batchlet and step listener) should be the same

        startJobAndWait(stepScopedTest);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(stepName1Repeat3, stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(stepName2, stepExecutions.get(1).getExitStatus());

        stepExecutions.clear();
        jobExecution = null;
        stepExecutions = null;

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
        Assert.assertEquals("[stepScopedPartitioned.step1, stepScopedPartitioned.step1, stepScopedPartitioned.step1, stepScopedPartitioned.step1, stepScopedPartitioned.step1]",
                stepExitStatus);
    }

    @Test
    public void stepScopedFail() throws Exception {
        //injecting @StepScoped Foo into a job listener will fail
        startJobAndWait(stepScopedFailedTest);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}