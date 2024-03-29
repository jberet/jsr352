/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.purgeInMemoryRepository;

import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.runtime.BatchStatus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PurgeInMemoryRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeInMemoryRepositoryXml = "purgeInMemoryRepository.xml";
    static final String transientUserDataXml = "transient-user-data.xml";

    @Test
    public void restartNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.restart(-1, null));
    }

    @Test
    public void stopNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.stop(-1));
    }

    @Test
    public void abandonNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.abandon(-1));
    }

    @Test
    public void getParametersNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getParameters(-1));
    }

    @Test
    public void getJobInstanceNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getJobInstance(-1));
    }

    @Test
    public void getStepExecutionsNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getStepExecutions(-1));
    }

    @Test
    public void getRunningExecutions() throws Exception {
        super.getRunningExecutions();
    }

    @Test
    public void getRunningExecutions2() throws Exception {
        super.getRunningExecutions2();
    }

    @Test
    public void getJobExecutionsByJob() throws Exception {
        super.getJobExecutionsByJob();
    }

    @Test
    public void getJobExecutionsByJobWithLimit() throws Exception {
        purgeJobExecutions();
        super.getJobExecutionsByJobWithLimit();
    }

    @Test
    public void jobExecutionSelector() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        params.setProperty("jobExecutionSelector",
                "org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase$JobExecutionSelector1");
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void purgeJobsByNamesAll() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();
        final long prepurge3JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("purgeJobsByNames", "*");
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
        assertNoSuchJobExecution(prepurge3JobExecutionId);
    }

    @Test
    public void jobExecutionsByJobNames() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();
        final long prepurge3JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("jobExecutionsByJobNames", prepurgeJobName);
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
        Assertions.assertNotNull(jobOperator.getJobExecution(prepurge3JobExecutionId));
    }

    @Test
    public void jobExecutionIds() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //non-existent job execution id 999 will be ignored
        params.setProperty("jobExecutionIds", String.format("%s,%s,%s", prepurge1JobExecutionId, prepurge2JobExecutionId, 999));
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }


    @Test
    public void numberOfRecentJobExecutionsToKeep() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //2 job executions to keep:
        //the purge job itself
        //prepurge2
        params.setProperty("numberOfRecentJobExecutionsToKeep", "2");
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        Assertions.assertNotNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void jobExecutionIdFrom() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id >= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        Assertions.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void jobExecutionIdFromIncludeRunningOnes() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id >= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        //include running job executions
        params.setProperty("keepRunningJobExecutions", String.valueOf(false));

        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        final long purgeJobExecutionId = prepurge2JobExecutionId + 1;
        assertNoSuchJobExecution(purgeJobExecutionId);

        Assertions.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void jobExecutionIdTo() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id <= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdTo", String.valueOf(prepurge2JobExecutionId));
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void jobExecutionIdFromTo() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();
        final long prepurge3JobExecutionId = prepurge();

        //purge job executions whose id between prepurge2JobExecutionId and prepurge3JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        params.setProperty("jobExecutionIdTo", String.valueOf(prepurge3JobExecutionId));
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        Assertions.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        assertNoSuchJobExecution(prepurge2JobExecutionId);
        assertNoSuchJobExecution(prepurge3JobExecutionId);
    }

    @Test
    public void withinPastMinutes() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions which ended within past 5 minutes
        params.setProperty("withinPastMinutes", "5");
        //keepRunningJobExecutions batch property defaults to true, the following sets it to its default value
        params.setProperty("keepRunningJobExecutions", String.valueOf(true));
        startAndVerifyPurgeJob(purgeInMemoryRepositoryXml);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    @Override
    public void noSuchJobException() throws Exception {
        super.noSuchJobException();
    }

    @Test
    @Override
    public void noSuchJobInstanceException() throws Exception {
        super.noSuchJobInstanceException();
    }

    @Test
    public void transientUserData() throws Exception {
        startJobAndWait(transientUserDataXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals("step persistent user data", stepExecution0.getPersistentUserData());
    }

    private void purgeJobExecutions() {
        jobOperator.getJobRepository().removeJobExecutions(new JobExecutionSelector1());
    }
}
