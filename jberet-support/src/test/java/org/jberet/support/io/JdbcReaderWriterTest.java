/*
 * Copyright (c) 2014-2017 Red Hat, Inc. and/or its affiliates.
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcReaderWriterTest {
    static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String h2JdbcDriverClassName = "org.h2.Driver";
    static final String writerTestJobName = "org.jberet.support.io.JdbcWriterTest";
    static final String readerTestJobName = "org.jberet.support.io.JdbcReaderTest";

    static final File dbDir = new File(CsvItemReaderWriterTest.tmpdir, "JdbcReaderWriterTest");
    static final String url = "jdbc:h2:" + dbDir.getPath();

    static final String dbUser = null;
    static final String dbPassword = null;

    static final String dropTable = "drop table STOCK_TRADE";
    static final String deleteAllRows = "delete from STOCK_TRADE";
    static final String createTable =
            "create table STOCK_TRADE (" +
                    "TRADEDATE TIMESTAMP NOT NULL, " +
                    "TRADETIME VARCHAR(30) NOT NULL, " +
                    "OPEN DOUBLE, " +
                    "HIGH DOUBLE, " +
                    "LOW DOUBLE, " +
                    "CLOSE DOUBLE, " +
                    "VOLUMN DOUBLE, " +
                    "PRIMARY KEY(TRADEDATE, TRADETIME))";
    static final String writerInsertSql =
            "insert into STOCK_TRADE (TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN) VALUES(?, ?, ?, ?, ?, ?, ?)";
    static final String writerInsertSql2 =
            "insert into STOCK_TRADE (TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE)         VALUES(?, ?, ?, ?, ?, ?)";

    static final String parameterTypes = "Date, String, Double, Double, Double, Double, Double";

    static final String readerQuery = "select TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN from STOCK_TRADE";
    static final String ibmStockTradeColumnsUpperCase = "TRADEDATE, TRADETIME, OPEN, HIGH, LOW, CLOSE, VOLUMN";
    static final String columnMapping = "date, time, open, high, low, close, volumn";
    static final String columnTypes = "Date, String, Double, Double, Double, Double, Double";
    static final String resultSetProperties =
    "fetchSize=1000, resultSetConcurrency=CONCUR_UPDATABLE, fetchDirection=FETCH_REVERSE, resultSetType=TYPE_SCROLL_SENSITIVE, resultSetHoldability=HOLD_CURSORS_OVER_COMMIT";

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
        //use StockTradeWithJoda for jdbc writer to test jackson-datatype-joda module
        //
        testWrite0(writerTestJobName, StockTrade.class, StockTradeWithJoda.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "10",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //columnMapping is provided for JdbcItemReaderWriter, so retrieved data will be keyed in the form of
        // "Date = xxx, Time = xxx, Open = xxx ..."
        // in StockTrade class, fields are mapped, with Jackson annotations, to "Date, Time, Open, ...", which
        // matches columnMapping above
        // CsvItemReaderWriter uses header "Date, Time, Open, ..."
        // CsvItemReaderWriter has nameMapping "date, time, open, ..." to match java fields in StockTrade. CsvItemReaderWriter
        // does not understand Jackson mapping annotations in POJO.

        //this JdbcItemReader only reads row 1 (start = 1 and end = 1 below)
        //use custom resultSetProperties
        //use StockTradeWithJoda for jdbc reader to test jackson-datatype-joda module
        //
        testRead0(readerTestJobName, StockTradeWithJoda.class, StockTrade.class, "readIBMStockTradeCsvWriteJdbcBeanType.out",
                "1", "1",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                readerQuery, ExcelWriterTest.ibmStockTradeHeader, parameterTypes, resultSetProperties,
                "09:30, 67040", "09:31");
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcMapType() throws Exception {
        testWrite0(writerTestJobName, Map.class, Map.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "120",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //no columnMapping provided for JdbcItemReader, so retrieved data will be keyed with database column names,
        //which is typically upper case
        //no nameMapping provided for CsvItemWriter, so the CsvItemWriter header will be used as nameMapping
        //CsvItemWriter header is all in upper case so as to match the data key, which is the database column names.

        //this JdbcItemReader reads all available rows (start = null and end = null below)
        //use default resultSetProperties
        testRead0(readerTestJobName, Map.class, Map.class, "readIBMStockTradeCsvWriteJdbcMapType.out",
                null, null,
                null, ibmStockTradeColumnsUpperCase,
                //readerQuery, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
                readerQuery, null, parameterTypes, null,
                "09:30, 67040,  1998-01-02,11:31,5900", "11:32");
    }

    /**
     * Same as {@link #readIBMStockTradeCsvWriteJdbcMapType}, except that this test uses stored procedure
     * in {@code jdbcItemReader}
     *
     * @throws Exception upon errors
     */
    @Test
    public void storedProcedureReader() throws Exception {
        final String storedProcedureDef = "CREATE ALIAS IF NOT EXISTS sp1 AS $$" +
                "ResultSet sp1(Connection conn, String sql) throws SQLException {" +
                "    return conn.createStatement().executeQuery(sql);" +
                "}$$;";
        final String callStoredProcedure = "{ call sp1('select * from STOCK_TRADE') }";

        final Connection connection = getConnection();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            final int result = statement.executeUpdate(storedProcedureDef);
            System.out.printf("Created stored procedure sp1 as %s%n", storedProcedureDef);
        } finally {
            JdbcItemReaderWriterBase.close(connection, statement);
        }

        testWrite0(writerTestJobName, Map.class, Map.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "120",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        testRead0(readerTestJobName, Map.class, Map.class, "readIBMStockTradeCsvWriteJdbcMapType.out",
                null, null,
                null, ibmStockTradeColumnsUpperCase,
                //readerQuery, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);
                callStoredProcedure, null, parameterTypes, null,
                "09:30, 67040,  1998-01-02,11:31,5900", "11:32");
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcListType() throws Exception {
        testWrite0(writerTestJobName, List.class, List.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "200",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //since beanType is List, data fields go by order, so CsvItemWriter does not need to do any mapping,
        //and so CsvItemWriter header is just used for display purpose only.

        //this JdbcItemReader reads row 2, 3, 4, 5 (start = 2 and end = 5 below)
        //use default resultSetProperties
        testRead0(readerTestJobName, List.class, List.class, "readIBMStockTradeCsvWriteJdbcListType.out",
                "2", "5",
                null, ExcelWriterTest.ibmStockTradeHeader,
                readerQuery, null, parameterTypes,
                "resultSetType=TYPE_SCROLL_INSENSITIVE",
                "09:31, 10810,  09:32, 09:33,  09:34, 4800", "09:35");
    }

    /**
     * Same as above test {@link #readIBMStockTradeCsvWriteJdbcListType()}, except that
     * in this test, the insert sql statement does not contain {@code VOLUMN} parameter.
     * While the incoming date item list still contains the {@code VOLUMN} value,
     * {@code jdbcItemWriter} should not attempt to assign that value to a non-exist
     * sql parameter.
     *
     * @see "https://issues.jboss.org/browse/JBERET-363"
     *
     * @throws Exception
     */
    @Test
    public void readIBMStockTradeCsvWriteJdbcListTypeNoVolumn() throws Exception {
        testWrite0(writerTestJobName, List.class, List.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "200",
                writerInsertSql2, null, parameterTypes);

        //since beanType is List, data fields go by order, so CsvItemWriter does not need to do any mapping,
        //and so CsvItemWriter header is just used for display purpose only.

        //this JdbcItemReader reads row 2, 3, 4, 5 (start = 2 and end = 5 below)
        //use default resultSetProperties
        testRead0(readerTestJobName, List.class, List.class, "readIBMStockTradeCsvWriteJdbcListType.out",
                "2", "5",
                null, ExcelWriterTest.ibmStockTradeHeader,
                readerQuery, null, parameterTypes,
                "resultSetType=TYPE_SCROLL_INSENSITIVE",
                "09:31,  09:32, 09:33,  09:34", "09:35, 10810, 4800");
    }

    @Test
    public void readIBMStockTradeCsvWriteJdbcListTypeAllRows() throws Exception {
        testWrite0(writerTestJobName, List.class, List.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "200",
                writerInsertSql, ExcelWriterTest.ibmStockTradeHeader, parameterTypes);

        //since beanType is List, data fields go by order, so CsvItemWriter does not need to do any mapping,
        //and so CsvItemWriter header is just used for display purpose only.

        //this JdbcItemReader reads all rows
        //use default resultSetProperties
        testRead0(readerTestJobName, List.class, List.class, "readIBMStockTradeCsvWriteJdbcListType.out",
                null, null,
                null, ExcelWriterTest.ibmStockTradeHeader,
                readerQuery, null, parameterTypes, null,
                "09:31, 10810,  09:32, 09:33,  09:34, 4800", null);
    }

    void testWrite0(final String jobName, final Class<?> readerBeanType, final Class<?> writerBeanType, final String csvNameMapping,
                    final String start, final String end,
                    final String sql, final String parameterNames, final String parameterTypes) throws Exception {
        // jdbc reader or writer may use org.jberet.support.io.StockTradeWithJoda to test custom module
        // jackson-datatype-joda, so use separate readerBeanType and writerBeanType
        final Properties params = new Properties();
        params.setProperty("readerBeanType", readerBeanType.getName());
        params.setProperty("writerBeanType", writerBeanType.getName());

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
        if (dbUser != null) {
            params.setProperty("user", dbUser);
        }
        if (dbPassword != null) {
            params.setProperty("password", dbPassword);
        }
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

    void testRead0(final String jobName, final Class<?> readerBeanType, final Class<?> writerBeanType, final String writeResource,
                   final String start, final String end,
                   final String csvNameMapping, final String csvHeader,
                   final String sql, final String columnMapping, final String columnTypes, final String resultSetProperties,
                   final String expect, final String forbid) throws Exception {

        // jdbc reader or writer may use org.jberet.support.io.StockTradeWithJoda to test custom module
        // jackson-datatype-joda, so use separate readerBeanType and writerBeanType
        final Properties params = new Properties();
        params.setProperty("readerBeanType", readerBeanType.getName());
        params.setProperty("writerBeanType", writerBeanType.getName());

        final File writeResourceFile;
        if (writeResource != null) {
            writeResourceFile = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
            params.setProperty("writeResource", writeResourceFile.getPath());
        } else {
            throw new RuntimeException("writeResource is null");
        }
        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (csvHeader != null) {
            params.setProperty("header", csvHeader);
        }

        params.setProperty("url", url);
        if (dbUser != null) {
            params.setProperty("user", dbUser);
        }
        if (dbPassword != null) {
            params.setProperty("password", dbPassword);
        }
        if (start != null) {
            params.setProperty("start", start);
        }
        if (end != null) {
            params.setProperty("end", end);
        }
        if (sql != null) {
            params.setProperty("sql", sql);
        }

        if (columnMapping != null) {
            params.setProperty("columnMapping", columnMapping);
        }
        if (columnTypes != null) {
            params.setProperty("columnTypes", columnTypes);
        }
        if (resultSetProperties != null) {
            params.setProperty("resultSetProperties", resultSetProperties);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        CsvItemReaderWriterTest.validate(writeResourceFile, expect, forbid);
    }

    static void initTable() throws Exception {
        Class.forName(h2JdbcDriverClassName);
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
        return dbUser == null ? DriverManager.getConnection(url) :
                DriverManager.getConnection(url, dbUser, dbPassword);
    }
}
