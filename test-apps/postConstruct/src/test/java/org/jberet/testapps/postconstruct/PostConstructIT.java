/*
 * Copyright (c) 2012-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.postconstruct;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PostConstructIT extends AbstractIT {
    @BeforeClass
    public static void beforeClass() {
        switchToUSLocale();
    }

    @AfterClass
    public static void afterClass() {
        restoreDefaultLocale();
    }

    @Test
    public void postConstructAndPreDestroy() throws Exception {
        final String expected = "PostConstructPreDestroyBase.ps JobListener1.ps JobListener1.beforeJob PostConstructPreDestroyBase.ps StepListener1.ps StepListener1.beforeStep PostConstructPreDestroyBase.ps Batchlet0.ps Batchlet1.ps Batchlet1.process Batchlet1.pd Batchlet0.pd PostConstructPreDestroyBase.pd StepListener1.afterStep StepListener1.pd PostConstructPreDestroyBase.pd PostConstructPreDestroyBase.ps Decider1.ps Decider1.decide Decider1.pd PostConstructPreDestroyBase.pd JobListener1.afterJob JobListener1.pd PostConstructPreDestroyBase.pd";
        startJobAndWait("postConstruct");
        final String jobExitStatus = jobExecution.getExitStatus();
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(expected, jobExitStatus);
    }

    /**
     * Verifies that after all steps are finished in a job execution, the job batch status is set to {@code COMPLETED}
     * when accessed in {@code javax.batch.api.listener.JobListener#afterJob()} method, and job execution {@code endTime}
     * is set to a valid date.
     *
     * If any {@code JobListener#afterJob} method fails, the job execution will still fail and batch status will change to
     * {@code FAILED}.
     *
     * The same should also be true for {@code javax.batch.api.listener.StepListener#afterStep()}.
     *
     * @throws Exception
     */
    @Test
    public void afterJobBatchStatus() throws Exception {
        startJobAndWait("afterJobBatchStatus");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
    }

    /**
     * When a job listener has wrong ref name in job xml, the job execution should
     * fail with {@code FAILED} batch status.
     *
     * @throws Exception
     */
    @Test
    public void wrongJobListenerName() throws Exception {
        startJobAndWait("wrongJobListenerName");
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        System.out.printf("Job exit status: %s%n", jobExecution.getExitStatus());
    }

    /**
     * Verifies that a step listener with wrong ref name in job xml should
     * cause the job execution to fail.
     *
     * @throws Exception
     */
    @Test
    public void wrongStepListenerName() throws Exception {
        startJobAndWait("wrongStepListenerName");
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        System.out.printf("Job exit status: %s%n", jobExecution.getExitStatus());
    }

    @Override
    protected long getJobTimeoutSeconds() {
        return 6;
    }
}
