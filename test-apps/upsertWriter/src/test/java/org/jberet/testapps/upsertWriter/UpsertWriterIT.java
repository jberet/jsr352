/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.upsertWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

/**
 * Tests using {@code jdbcItemWriter} in jberet-support, and upsert / merge
 * sql statement.
 */
@Ignore("Need to connect to db2 database server")
public class UpsertWriterIT extends AbstractIT {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String upsertWriterTestJob = "upsertWriterTest";

    static final String url = "";
    static final String user = "";
    static final String password = "";

    //rank,tit,grs,opn
    static final String createTable =
"CREATE TABLE MOVIES (" +
"  rank VARCHAR(512) NOT NULL CONSTRAINT MOVIES_PK PRIMARY KEY," +
"  tit  VARCHAR(512)," +
"  grs  VARCHAR(512)," +
"  opn  VARCHAR(512)" +
")";

    static final String dropTable = "drop table MOVIES";

    static final String insertRecord =
"insert into MOVIES (rank,tit,grs,opn) VALUES ('1', 'TBD', 'TBD', 'TBD'), ('9999', 'TBD', 'TBD', 'TBD')";

    static final String selectAll = "select * from MOVIES";

    /**
     * Initializes database:
     * <ul>
     *     <li>drop table
     *     <li>create table
     *     <li>insert 2 rows, 1 row is to be merged with batch data item when running
     *     {@link #upsertWriterTest()}, and the other row is not modified
     *
     * @throws Exception upon errors
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection(url, user, password);

            preparedStatement = connection.prepareStatement(dropTable);
            try {
                final boolean dropResult = preparedStatement.execute();
                if (dropResult) {
                    System.out.printf("Dropped table: %s%n", dropTable);
                } else {
                    System.out.printf("Failed to drop table: %s%n", dropTable);
                }
            } catch (Exception e) {
                System.out.printf("Failed to drop table, which may not be present%n%s%n", e.toString());
            }

            preparedStatement = connection.prepareStatement(createTable);
            final boolean createResult = preparedStatement.execute();
            if (createResult) {
                System.out.printf("Created table: %s%n", createTable);
            } else {
                System.out.printf("Failed to create table %s%n", createTable);
            }

            preparedStatement = connection.prepareStatement(insertRecord);
            final int insertResult = preparedStatement.executeUpdate();
            System.out.printf("Inserted %s records%n", insertResult);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    @Test
    public void upsertWriterTest() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("url", url);
        jobParams.setProperty("user", user);
        jobParams.setProperty("password", password);

        final long jobExecutionId = jobOperator.start(upsertWriterTestJob, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);

        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        System.out.printf("Records after running tests:%n%s%n", selectAll());
    }

    /**
     * Selects all records from the table and append all data to string.
     *
     * @return the result string containing all records
     * @throws Exception upon errors
     */
    protected String selectAll() throws Exception {
        StringBuilder sb = new StringBuilder();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet r = null;

        try {
            connection = DriverManager.getConnection(url, user, password);
            preparedStatement = connection.prepareStatement(selectAll);
            r = preparedStatement.executeQuery();
            while (r.next()) {
                sb.append(r.getString(1)).append("\t");
                sb.append(r.getString(2)).append("\t");
                sb.append(r.getString(3)).append("\t");
                sb.append(r.getString(4)).append("\n");
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception e) {}
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {}
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {}
            }
        }
        return sb.toString();
    }
}
