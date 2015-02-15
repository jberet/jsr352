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

import java.util.List;
import javax.batch.operations.NoSuchJobException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;

import org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase;
import org.junit.Assert;
import org.junit.Test;

public class PurgeJdbcRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeJdbcRepositoryXml = "purgeJdbcRepository";

    @Test
    public void withSql() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2Xml);

        params.setProperty("sql",
        "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
            "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

        "delete from JOB_EXECUTION where JOBEXECUTIONID in " +
            "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%');"
        );

        params.setProperty("jobExecutionsByJobNames", "prepurge, prepurge2");

        startAndVerifyPurgeJob(purgeJdbcRepositoryXml);

        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void deleteJobInstancesWithSql() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2Xml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge1JobExecutionId).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge2JobExecutionId).getBatchStatus());
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurgeXml));
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurge2Xml));
        Assert.assertNotNull(jobOperator.getJobInstances(prepurgeXml, 0, 1).get(0));
        Assert.assertNotNull(jobOperator.getJobInstances(prepurge2Xml, 0, 1).get(0));

        params.setProperty("sql",
        "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
            "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

        "delete from JOB_EXECUTION where JOBEXECUTIONID in " +
            "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

        "delete from JOB_INSTANCE where JOBNAME like 'prepurge%' "
        );

        params.setProperty("jobExecutionsByJobNames", "prepurge, prepurge2");

        startAndVerifyPurgeJob(purgeJdbcRepositoryXml);

        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge2JobExecutionId));
        Assert.assertEquals(0, jobOperator.getJobInstanceCount(prepurgeXml));
        Assert.assertEquals(0, jobOperator.getJobInstanceCount(prepurge2Xml));
        Assert.assertEquals(0, jobOperator.getJobInstances(prepurgeXml, 0, 1).size());
        Assert.assertEquals(0, jobOperator.getJobInstances(prepurge2Xml, 0, 1).size());
    }

    @Test
    public void noSuchJobException() throws Exception {
        try {
            final int result = jobOperator.getJobInstanceCount("no-such-job");
            Assert.fail("Expecting NoSuchJobException, but got " + result);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            final List<JobInstance> result = jobOperator.getJobInstances("no-such-job", 0, 1);
            Assert.fail("Expecting NoSuchJobException, but got " + result);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void withSqlFile() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2Xml);

        params.setProperty("sqlFile", "purgeJdbcRepository.sql");

        //prepurge2 job execution is purged from in-memory part, but still kept in database.
        //So next when calling getJobExecution(prepurge2JobExecutionId) should retrieve it from the database, and return
        //non-null.
        params.setProperty("jobExecutionsByJobNames", "prepurge, prepurge2");

        startAndVerifyPurgeJob(purgeJdbcRepositoryXml);

        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNotNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

}
