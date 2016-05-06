/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.purgeJdbcRepository2;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests to verify {@code PurgeBatchlet} in JBoss EAP and WildFly.
 *
 * Similar tests for Java SE environment are:
 * <ul>
 * <li>test-apps/purgeJdbcRepository
 * <li>test-apps/purgeInMemoryRepository
 * <li>test-apps/purgeMongoRepository
 * </ul>
 */
public final class PurgeJdbcRepository2IT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/purgeJdbcRepository2.xml}
     */
    private static final String jobName = "purgeJdbcRepository2";

    /**
     * The job name defined in {@code META-INF/batch-jobs/prepurge2.xml}
     */
    private static final String prepurge2JobName = "prepurge2";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "purgeJdbcRepository2/api";

    private BatchClient batchClient = new BatchClient(restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void withSql() throws Exception {
        final long prepurgeExecutionId = prepurge();
        JobExecutionEntity prepurgeJobExecution = batchClient.getJobExecution(prepurgeExecutionId);
        assertEquals(BatchStatus.COMPLETED, prepurgeJobExecution.getBatchStatus());

        final String sql =
        "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
            "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
            "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

        "delete from JOB_EXECUTION where JOBINSTANCEID in " +
            "(select JOBINSTANCEID from JOB_INSTANCE where JOBNAME like 'prepurge%');";


        final Properties jobParams = new Properties();
        jobParams.setProperty("sql", sql);
        startJobCheckStatus(jobName, jobParams, 3000, BatchStatus.COMPLETED);

//        prepurgeJobExecution = batchClient.getJobExecution(prepurgeExecutionId);
//        assertEquals(null, prepurgeJobExecution);
    }

    @Test
    public void withSqlFile() throws Exception {
        final long prepurgeExecutionId = prepurge();
        JobExecutionEntity prepurgeJobExecution = batchClient.getJobExecution(prepurgeExecutionId);
        assertEquals(BatchStatus.COMPLETED, prepurgeJobExecution.getBatchStatus());

        final String sqlFile = "purgeJdbcRepository2.sql";
        final Properties jobParams = new Properties();
        jobParams.setProperty("sqlFile", sqlFile);
        startJobCheckStatus(jobName, jobParams, 3000, BatchStatus.COMPLETED);
    }

    private long prepurge() throws Exception {
        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(prepurge2JobName, null);
        Thread.sleep(1000);
        return jobExecutionEntity.getExecutionId();
    }

}
