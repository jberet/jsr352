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

package org.jberet.testapps.cdiscopes.jobscoped;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class JobScopedIT extends AbstractIT {
    private static final String jobScopedTest = "jobScoped";
    private static final String jobScopedTest2 = "jobScoped2";
    private static final String jobScopedPartitionedTest = "jobScopedPartitioned";
    private static final String jobScopedPartitionedTest2 = "jobScoped2Partitioned";

    @Test
    public void jobScopedTest() throws Exception {
        startJobAndWait(jobScopedTest);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals("jobScoped.step1TYPE jobScoped.step1METHOD jobScoped.step1FIELD",
                stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(
"jobScoped.step1TYPE jobScoped.step2TYPE jobScoped.step1METHOD jobScoped.step2METHOD jobScoped.step1FIELD jobScoped.step2FIELD",
                stepExecutions.get(1).getExitStatus());

        stepExecutions.clear();
        jobExecution = null;
        stepExecutions = null;

        //run jobScoped2 to check that a different Foo instance is used within the scope of jobScoped2
        startJobAndWait(jobScopedTest2);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals("jobScoped2.step1TYPE jobScoped2.step1METHOD jobScoped2.step1FIELD",
                stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(
"jobScoped2.step1TYPE jobScoped2.step2TYPE jobScoped2.step1METHOD jobScoped2.step2METHOD jobScoped2.step1FIELD jobScoped2.step2FIELD",
                stepExecutions.get(1).getExitStatus());
    }

    @Test
    public void jobScopedPartitionedTest() throws Exception {
        startJobAndWait(jobScopedPartitionedTest);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        //step2 exit status is set in analyzer, to foo.stepNames, which should include values from both step1 and 2.
        String step2ExitStatus = stepExecutions.get(1).getExitStatus();
        Assert.assertEquals("jobScopedPartitioned.step1TYPE jobScopedPartitioned.step1TYPE jobScopedPartitioned.step2TYPE jobScopedPartitioned.step2TYPE jobScopedPartitioned.step1METHOD jobScopedPartitioned.step1METHOD jobScopedPartitioned.step2METHOD jobScopedPartitioned.step2METHOD jobScopedPartitioned.step1FIELD jobScopedPartitioned.step1FIELD jobScopedPartitioned.step2FIELD jobScopedPartitioned.step2FIELD",
                step2ExitStatus);

        stepExecutions.clear();
        jobExecution = null;
        stepExecutions = null;
        step2ExitStatus = null;

        //run jobScoped2 to check that a different Foo instance is used within the scope of jobScoped2
        startJobAndWait(jobScopedPartitionedTest2);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        step2ExitStatus = stepExecutions.get(1).getExitStatus();
        Assert.assertEquals("jobScoped2Partitioned.step1TYPE jobScoped2Partitioned.step1TYPE jobScoped2Partitioned.step2TYPE jobScoped2Partitioned.step2TYPE jobScoped2Partitioned.step1METHOD jobScoped2Partitioned.step1METHOD jobScoped2Partitioned.step2METHOD jobScoped2Partitioned.step2METHOD jobScoped2Partitioned.step1FIELD jobScoped2Partitioned.step1FIELD jobScoped2Partitioned.step2FIELD jobScoped2Partitioned.step2FIELD",
                step2ExitStatus);
    }
}