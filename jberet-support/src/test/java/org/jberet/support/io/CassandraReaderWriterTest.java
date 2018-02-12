/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
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

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Need to run Cassandra cluster first")
public class CassandraReaderWriterTest {
    static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.CassandraWriterTest";
    static final String contactPoints = "localhost";
    static final String keyspace = "test";
    static Cluster cluster;
    static Session session;

    static final String dropKeyspace = "drop keyspace if exists " + keyspace;
    static final String dropTable = "drop table stock_trade";
    static final String deleteAllRows = "truncate stock_trade";

    static final String createKeyspace = "create keyspace if not exists " + keyspace +
            " with replication = {'class': 'SimpleStrategy', 'replication_factor' : 1}";

    static final String useKeyspace = "use " + keyspace;

    static final String createTable =
            "create table if not exists stock_trade (" +
                    "tradedate timestamp, " +
                    "tradetime text, " +
                    "open double, " +
                    "high double, " +
                    "low double, " +
                    "close double, " +
                    "volume double, " +
                    "primary key(tradedate, tradetime))";

    static final String writerInsertCql =
    "insert into stock_trade (tradedate, tradetime, open, high, low, close, volume) values(?, ?, ?, ?, ?, ?, ?)";

    static final String writerInsertCql2 =
    "insert into stock_trade (tradedate, tradetime, open, high, low, close, volume) " +
    "values(:date, :time, :open, :high, :low, :close, :volume)";

    @BeforeClass
    public static void beforeClass() {
        initKeyspaceAndTable();
    }

    @AfterClass
    public static void afterClass() {
        if (session != null) {
            session.close();
        }
    }

    @Before
    public void before() {
        deleteAllRows();
    }

    @Test
    public void readIBMStockTradeCsvWriteCassandraList() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("readerBeanType", java.util.List.class.getName());
        jobParams.setProperty("contactPoints", contactPoints);
        jobParams.setProperty("keyspace", keyspace);
        jobParams.setProperty("cql", writerInsertCql);
        jobParams.setProperty("end", String .valueOf(5));  // read the first 5 lines

        final long jobExecutionId = jobOperator.start(writerTestJobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    @Test
    public void readIBMStockTradeCsvWriteCassandraMap() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("readerBeanType", java.util.Map.class.getName());
        jobParams.setProperty("contactPoints", contactPoints);
        jobParams.setProperty("keyspace", keyspace);
        jobParams.setProperty("cql", writerInsertCql2);  // use cql with named parameters
        jobParams.setProperty("end", String .valueOf(10));  // read the first 10 lines

        //use nameMapping since the bean type is Map, and the input csv has no header
        jobParams.setProperty("nameMapping", ExcelWriterTest.ibmStockTradeNameMapping);

        final long jobExecutionId = jobOperator.start(writerTestJobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(1, TimeUnit.MINUTES);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    static void initKeyspaceAndTable() {
        final ResultSet resultSet = getSession().execute(createKeyspace);
        if (resultSet.wasApplied()) {
            System.out.printf("Created keyspace %s%n", keyspace);
        }
        getSession().execute(useKeyspace);
        final ResultSet resultSet1 = getSession().execute(createTable);
        if (resultSet1.wasApplied()) {
            System.out.printf("Created table STOCK_TRADE%n");
        }
    }

    static void deleteAllRows() {
        final ResultSet resultSet = getSession().execute(deleteAllRows);
        System.out.printf("Deleted rows: %s%n", resultSet.one());
    }

    static Session getSession() {
        if (session != null) {
            return session;
        }
        if(cluster == null) {
            cluster = new Cluster.Builder().addContactPoint(contactPoints).build();
        }

        return session = cluster.newSession();
    }
}
