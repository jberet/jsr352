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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcReaderWriterTest {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.JdbcWriterTest.xml";
    static final String readerTestJobName = "org.jberet.support.io.JdbcReaderTest.xml";

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

    static final String readerQuery = "select TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN from STOCK_TRADE";
    static final String ibmStockTradeColumnsUpperCase = "TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN";
    static final String columnMapping = "date, time, open, high, low, close, volumn";
    static final String columnTypes = "Date, String, Double, Double, Double, Double, Double";

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
        testWrite0(writerTestJobName, StockTrade.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "10",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //columnMapping is provided for JdbcItemReaderWriter, so retrieved data will be keyed in the form of
        // "Date = xxx, Time = xxx, Open = xxx ..."
        // in StockTrade class, fields are mapped, with Jackson annotations, to "Date, Time, Open, ...", which
        // matches columnMapping above
        // CsvItemReaderWriter uses header "Date, Time, Open, ..."
        // CsvItemReaderWriter has nameMapping "date, time, open, ..." to match java fields in StockTrade. CsvItemReaderWriter
        // does not understand Jackson mapping annotations in POJO.
        testRead0(readerTestJobName, StockTrade.class, "readIBMStockTradeCsvWriteJdbcBeanType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                readerQuery, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcMapType() throws Exception {
        testWrite0(writerTestJobName, Map.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "120",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //no columnMapping provided for JdbcItemReader, so retrieved data will be keyed with database column names,
        //which is typically upper case
        //no nameMapping provided for CsvItemWriter, so the CsvItemWriter header will be used as nameMapping
        //CsvItemWriter header is all in upper case so as to match the data key, which is the database column names.
        testRead0(readerTestJobName, Map.class, "readIBMStockTradeCsvWriteJdbcMapType.out",
                null, ibmStockTradeColumnsUpperCase,
                //readerQuery, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
                readerQuery, null, parameterTypes);
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcListType() throws Exception {
        testWrite0(writerTestJobName, List.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "200",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //since beanType is List, data fields go by order, so CsvItemWriter does not need to do any mapping,
        //and so CsvItemWriter header is just used for display purpose only.
        testRead0(readerTestJobName, List.class, "readIBMStockTradeCsvWriteJdbcListType.out",
                null, ExcelWriterTest.ibmStockTradeHeader,
                readerQuery, null, parameterTypes);
    }

    void testWrite0(final String jobName, final Class<?> beanType, final String csvNameMapping,
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

    void testRead0(final String jobName, final Class<?> beanType, final String writeResource,
                   final String csvNameMapping, final String csvHeader,
                    final String sql, final String columnMapping, final String columnTypes) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        if (writeResource != null) {
            params.setProperty("writeResource", new File(CsvItemReaderWriterTest.tmpdir, writeResource).getPath());
        }
        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (csvHeader != null) {
            params.setProperty("header", csvHeader);
        }

        params.setProperty("url", url);
        if (sql != null) {
            params.setProperty("sql", sql);
        }

        if (columnMapping != null) {
            params.setProperty("columnMapping", columnMapping);
        }
        if (columnTypes != null) {
            params.setProperty("columnTypes", columnTypes);
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
        JdbcItemReaderWriterBase.close(connection, preparedStatement);

        connection = getConnection();
        preparedStatement = connection.prepareStatement(createTable);
        preparedStatement.execute();
        JdbcItemReaderWriterBase.close(connection, preparedStatement);
    }

    static void deleteAllRows() throws Exception {
        final Connection connection = getConnection();
        final PreparedStatement preparedStatement = connection.prepareStatement(deleteAllRows);
        try {
            preparedStatement.execute();
        } catch (final SQLException e) {
            //igore
        }
        JdbcItemReaderWriterBase.close(connection, preparedStatement);
    }

    static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url);
    }
}
