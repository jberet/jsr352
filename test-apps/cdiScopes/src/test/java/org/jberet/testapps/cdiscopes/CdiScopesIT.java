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

package org.jberet.testapps.cdiscopes;

import java.util.Arrays;
import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class CdiScopesIT extends AbstractIT {
    static final String jobScopedTest = "jobScoped";
    static final String jobScopedTest2 = "jobScoped2";

    static final String stepName1 = Arrays.asList((new String[]{"jobScoped.step1"})).toString();
    static final String step1Step2Names = Arrays.asList((new String[]{"jobScoped.step1", "jobScoped.step2"})).toString();

    static final String job2StepName1 = Arrays.asList((new String[]{"jobScoped2.step1"})).toString();
    static final String job2Step1Step2Names = Arrays.asList((new String[]{"jobScoped2.step1", "jobScoped2.step2"})).toString();


    @Test
    public void jobScopedTest() throws Exception {
        startJobAndWait(jobScopedTest);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(stepName1, stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(step1Step2Names, stepExecutions.get(1).getExitStatus());

        stepExecutions.clear();
        jobExecution = null;
        stepExecutions = null;

        //run jobScoped2 to check that a different Foo instance is used within the scope of jobScoped2
        startJobAndWait(jobScopedTest2);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(job2StepName1, stepExecutions.get(0).getExitStatus());
        Assert.assertEquals(job2Step1Step2Names, stepExecutions.get(1).getExitStatus());

    }
}
