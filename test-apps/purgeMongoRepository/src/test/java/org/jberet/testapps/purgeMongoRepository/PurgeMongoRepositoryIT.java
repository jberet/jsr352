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

package org.jberet.testapps.purgeMongoRepository;

import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;

import org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase;
import org.junit.Assert;
import org.junit.Test;

public class PurgeMongoRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeMongoRepositoryJobName = "purgeMongoRepository";

    @Test(expected = NoSuchJobExecutionException.class)
    public void restartNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.restart(-1, null);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void stopNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.stop(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void abandonNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.abandon(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void getParametersNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.getParameters(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void getJobInstanceNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.getJobInstance(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void getStepExecutionsNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.getStepExecutions(-1);
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
    public void getRunningExecutions() throws Exception {
        super.getRunningExecutions();
    }

    @Test
    public void getRunningExecutions2() throws Exception {
        super.getRunningExecutions2();
    }

    @Test
    public void removeStepExecutionsAndJobExecutions() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}}); " +
                        "db. JOB_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}})");

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void removeStepExecutionsAndJobExecutionsAndJobInstances() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);
        final long instanceId1 = jobOperator.getJobInstance(prepurge1JobExecutionId).getInstanceId();
        final long instanceId2 = jobOperator.getJobInstance(prepurge2JobExecutionId).getInstanceId();

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}}); " +
                "db. JOB_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}}); " +
                "db.  JOB_INSTANCE.remove({JOBINSTANCEID:  {$in: [" + instanceId1 + ", " + instanceId2 + "]}})"
        );

        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        try {
            final JobInstance ins = jobOperator.getJobInstance(prepurge1JobExecutionId);
            org.junit.Assert.fail("Expecting NoSuchJobExecutionException, but got " + ins);
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
        try {
            final JobInstance ins = jobOperator.getJobInstance(prepurge2JobExecutionId);
            org.junit.Assert.fail("Expecting NoSuchJobException, but got " + ins);
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }

    }

    @Test
    public void removeAll() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);
        final long instanceId1 = jobOperator.getJobInstance(prepurge1JobExecutionId).getInstanceId();
        final long instanceId2 = jobOperator.getJobInstance(prepurge2JobExecutionId).getInstanceId();

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove(); " +
                "db. JOB_EXECUTION.remove(); " +
                "db.  JOB_INSTANCE.remove()"
        );

        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        try {
            final int count = jobOperator.getJobInstanceCount(prepurgeJobName);
            org.junit.Assert.fail("Expecting NoSuchJobException, but got " + count);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }
        try {
            final int count = jobOperator.getJobInstanceCount(prepurge2JobName);
            org.junit.Assert.fail("Expecting NoSuchJobException, but got " + count);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void invalidRemoveQueries() throws Exception {
        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove(1); " +
                "db. JOB_EXECUTION.remove(1); " +
                "db.  JOB_INSTANCE.remove(1)"
        );

        startJob(purgeMongoRepositoryJobName);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}
