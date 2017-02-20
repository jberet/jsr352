/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jberet.support.io.JdbcReaderWriterTest.jobOperator;
import static org.junit.Assert.assertEquals;

public class JdbcBatchletTest {
    static final String jdbcBatchletJobName = "org.jberet.support.io.JdbcBatchletTest";
    static final String insertSql =
            "insert into STOCK_TRADE (TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN) VALUES" +
                    "('2017-02-17','15:51',122.25,122.44,122.25,122.44,16800.0)";

    static final String sqls = insertSql + ";" + JdbcReaderWriterTest.deleteAllRows;

    @BeforeClass
    public static void beforeClass() throws Exception {
        JdbcReaderWriterTest.initTable();
    }

    @Test
    public void multipleSqls() throws Exception {
        runTest(sqls, BatchStatus.COMPLETED);
    }

    @Test
    public void singleSql() throws Exception {
        runTest(JdbcReaderWriterTest.readerQuery, BatchStatus.COMPLETED);
    }

    @Test
    public void multipleSqlsInvalid() throws Exception {
        runTest(sqls + ";" + "xxx", BatchStatus.FAILED);
    }

    private void runTest(final String sqls, final BatchStatus batchStatus) throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("sqls", sqls);
        jobParams.setProperty("url", JdbcReaderWriterTest.url);
        final long jobExecutionId = jobOperator.start(jdbcBatchletJobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(batchStatus, jobExecution.getBatchStatus());
        assertEquals(batchStatus.toString(), jobExecution.getExitStatus());

        if (batchStatus == BatchStatus.FAILED) {
            return;
        }

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        System.out.printf("Step exit status (sqls execution result): %s%n", stepExecutions.get(0).getExitStatus());
    }
}
