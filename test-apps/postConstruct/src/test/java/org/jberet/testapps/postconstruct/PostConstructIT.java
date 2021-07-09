/*
 * Copyright (c) 2012-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.postconstruct;

import static org.junit.Assert.assertEquals;

import jakarta.batch.runtime.BatchStatus;
import org.jberet.testapps.common.AbstractIT;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PostConstructIT extends AbstractIT {
    @BeforeClass
    public static void beforeClass() {
        switchToUSLocale();
    }

    @AfterClass
    public static void afterClass() {
        restoreDefaultLocale();
    }

    /**
     * Verifies @PostConstruct and @PreDestroy methods are invoked for item reader,
     * item processor, and item writer artifacts in a chunk step.
     * Each of these lifecycle methods adds a string to job exit status to identify
     * the current method.
     * <p>
     * The item reader (ref name: itemReader1) is named with its default CDI bean name.
     * <p>
     * The item processor (ref name: org.jberet.testapps.postconstruct.ItemProcessor1)
     * is named with its fully-qualified class name.
     * <p>
     * The item writer (ref name: W1) is declared in batch.xml.
     * <p>
     * For all 3 artifacts, their @PostConstruct and @PreDestroy methods should be
     * invoked.
     *
     * @throws Exception if errors
     */
    @Test
    public void chunkPostConstructPreDestroy() throws Exception {
        final String expected = "ItemReader1.postConstruct ItemWriter1.postConstruct ItemProcessor1.postConstruct ItemReader1.preDestroy ItemWriter1.preDestroy ItemProcessor1.preDestroy";
        startJobAndWait("chunkPostConstruct");
        final String jobExitStatus = jobExecution.getExitStatus();
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(expected, jobExitStatus);
    }

    /**
     * Verifies @PostConstruct and @PreDestroy methods are invoked for batchlet,
     * decider, job listener and step listener artifacts in a batchlet step.
     * Each of these lifecycle methods adds a string to job exit status to identify
     * the current method.
     *
     * @throws Exception if errors
     */
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
     * when accessed in {@code jakarta.batch.api.listener.JobListener#afterJob()} method, and job execution {@code endTime}
     * is set to a valid date.
     *
     * If any {@code JobListener#afterJob} method fails, the job execution will still fail and batch status will change to
     * {@code FAILED}.
     *
     * The same should also be true for {@code jakarta.batch.api.listener.StepListener#afterStep()}.
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
