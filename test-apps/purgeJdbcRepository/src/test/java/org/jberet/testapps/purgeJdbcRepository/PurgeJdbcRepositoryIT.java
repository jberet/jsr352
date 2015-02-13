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

import org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase;
import org.junit.Assert;
import org.junit.Test;

public class PurgeJdbcRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeJdbcRepositoryXml = "purgeJdbcRepository.xml";

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
