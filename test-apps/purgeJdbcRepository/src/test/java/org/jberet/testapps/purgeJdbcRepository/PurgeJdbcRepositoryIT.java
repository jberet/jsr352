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

package org.jberet.testapps.purgeJdbcRepository;

import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase;
import org.junit.Assert;
import org.junit.Test;

public class PurgeJdbcRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeJdbcRepositoryJobName = "purgeJdbcRepository";

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
    public void withSql() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sql",
                "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
                        "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

                        "delete from JOB_EXECUTION where JOBEXECUTIONID in " +
                        "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%');"
        );

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void deleteJobInstancesWithSql() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge1JobExecutionId).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge2JobExecutionId).getBatchStatus());
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurgeJobName));
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurge2JobName));
        Assert.assertNotNull(jobOperator.getJobInstances(prepurgeJobName, 0, 1).get(0));
        Assert.assertNotNull(jobOperator.getJobInstances(prepurge2JobName, 0, 1).get(0));
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurgeJobName).size());
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurge2JobName).size());

        params.setProperty("sql",
                "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
                        "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

                        "delete from JOB_EXECUTION where JOBEXECUTIONID in " +
                        "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

                        "delete from JOB_INSTANCE where JOBNAME like 'prepurge%' "
        );

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
        Assert.assertEquals(0, jobOperator.getJobInstanceCount(prepurgeJobName));
        Assert.assertEquals(0, jobOperator.getJobInstanceCount(prepurge2JobName));
        Assert.assertEquals(0, jobOperator.getJobInstances(prepurgeJobName, 0, 1).size());
        Assert.assertEquals(0, jobOperator.getJobInstances(prepurge2JobName, 0, 1).size());
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurgeJobName).size());
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurge2JobName).size());
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
    public void withSqlFile() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sqlFile", "purgeJdbcRepository.sql");

        //prepurge2 job execution is purged from in-memory part, but still kept in database.
        //So next when calling getJobExecution(prepurge2JobExecutionId) should retrieve it from the database, and return
        //non-null.
        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge2JobExecutionId).getBatchStatus());
    }

    @Test
    public void withSqlDeleteJobInstancesCascade() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sql", "delete from JOB_INSTANCE where JOBNAME like 'prepurge%'");
        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        try {
            Assert.fail("Expecting NoSuchJobException, but got " + jobOperator.getJobInstanceCount(prepurgeJobName));
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobException, but got " + jobOperator.getJobInstanceCount(prepurge2JobName));
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge1JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge2JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void withSqlDeleteJobExecutionsCascade() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sql",
                "delete from JOB_EXECUTION where JOBEXECUTIONID in " +
                    "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); ");

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurgeJobName));
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurge2JobName));

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge1JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge2JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

}
