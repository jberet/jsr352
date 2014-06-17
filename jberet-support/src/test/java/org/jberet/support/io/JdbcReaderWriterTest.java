/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.support._private.SupportLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcReaderWriterTest {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.JdbcWriterTest.xml";

    static final File dbDir = new File(CsvItemReaderWriterTest.tmpdir, "JdbcReaderWriterTest");
    static final String url = "jdbc:h2:" + dbDir.getPath();
    static final String dropTable = "drop table STOCK_TRADE";
    static final String deleteAllRows = "delete from STOCK_TRADE";
    static final String createTable =
                    "create table STOCK_TRADE (" +
                            "TRADEDATE TIMESTAMP, " +
                            "TRADETIME VARCHAR(30), " +
                            "OPEN DOUBLE, " +
                            "HIGH DOUBLE, " +
                            "LOW DOUBLE, " +
                            "CLOSE DOUBLE, " +
                            "VOLUMN DOUBLE, " +
                    "PRIMARY KEY(TRADEDATE, TRADETIME))";
    static final String writerInsertSql =
            "insert into STOCK_TRADE (TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN) VALUES(?, ?, ?, ?, ?, ?, ?)";

    static final String parameterTypes = "Date, String, Double, Double, Double, Double, Double";

    @BeforeClass
    public static void beforeClass() throws Exception {
        initTable();
    }

    @Before
    public void before() throws Exception {
        deleteAllRows();
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcBeanType() throws Exception {
        testReadWrite0(writerTestJobName, StockTrade.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "10",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcMapType() throws Exception {
        testReadWrite0(writerTestJobName, Map.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "120",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcListType() throws Exception {
        testReadWrite0(writerTestJobName, List.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "200",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
    }

    void testReadWrite0(final String jobName, final Class<?> beanType, final String csvNameMapping,
                        final String start, final String end,
                        final String sql, final String parameterNames, final String parameterTypes) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (start != null) {
            params.setProperty("start", start);
        }
        if (end != null) {
            params.setProperty("end", end);
        }

        params.setProperty("url", url);
        if (sql != null) {
            params.setProperty("sql", sql);
        }

        if (parameterNames != null) {
            params.setProperty("parameterNames", parameterNames);
        }
        if (parameterTypes != null) {
            params.setProperty("parameterTypes", parameterTypes);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    static void initTable() throws Exception {
        Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(dropTable);
        try {
            preparedStatement.execute();
        } catch (final SQLException e) {
            //igore, since the table may not exist
        }
        close(connection, preparedStatement);

        connection = getConnection();
        preparedStatement = connection.prepareStatement(createTable);
        preparedStatement.execute();
        close(connection, preparedStatement);
    }

    static void deleteAllRows() throws Exception {
        final Connection connection = getConnection();
        final PreparedStatement preparedStatement = connection.prepareStatement(deleteAllRows);
        try {
            preparedStatement.execute();
        } catch (final SQLException e) {
            //igore
        }
        close(connection, preparedStatement);
    }

    static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url);
    }

    static void close(final Connection connection, final Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (final SQLException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close statement %s%n", statement);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (final SQLException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close connection %s%n", connection);
            }
        }
    }

}
