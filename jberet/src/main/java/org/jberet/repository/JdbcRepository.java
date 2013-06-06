/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012-2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jberet.config.BatchConfig;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;

public final class JdbcRepository extends AbstractRepository {
    //keys used in jberet.properties
    public static final String DDL_FILE_NAME_KEY = "ddl-file";
    public static final String SQL_FILE_NAME_KEY = "sql-file";
    public static final String DATASOURCE_JNDI_KEY = "datasource-jndi";
    public static final String DB_URL_KEY = "db-url";
    public static final String DB_USER_KEY = "db-user";
    public static final String DB_PASSWORD_KEY = "db-password";
    public static final String DB_PROPERTIES_KEY = "db-properties";
    public static final String DB_PROPERTY_DELIM = ":";

    //defaults for entries in jberet.properties
    private static final String DEFAULT_DATASOURCE = "java:jboss/datasources/ExampleDS";
    //    private static final String DEFAULT_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DEFAULT_DB_URL = "jdbc:h2:~/jberet-repo";
    private static final String DEFAULT_SQL_FILE = "sql/jberet-h2-sql.properties";
    private static final String DEFAULT_DDL_FILE = "sql/jberet-h2.ddl";

    //keys used in *.sql files
    private static final String SELECT_ALL_JOB_INSTANCES = "select-all-job-instances";
    private static final String SELECT_JOB_INSTANCE = "select-job-instance";
    private static final String INSERT_JOB_INSTANCE = "insert-job-instance";

    private static final String SELECT_ALL_JOB_EXECUTIONS = "select-all-job-executions";
    private static final String SELECT_JOB_EXECUTION = "select-job-execution";
    private static final String INSERT_JOB_EXECUTION = "insert-job-execution";

    private static final String SELECT_ALL_STEP_EXECUTIONS = "select-all-step-executions";
    private static final String SELECT_STEP_EXECUTION = "select-step-execution";
    private static final String INSERT_STEP_EXECUTION = "insert-step-execution";

    private Properties configProperties;
    private Context namingContext;
    private String dataSourceName;
    private DataSource dataSource;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Properties dbProperties;
    private Properties sqls = new Properties();

    private static class Holder {
        private static final JdbcRepository instance = new JdbcRepository();
    }

    static JdbcRepository getInstance() {
        return Holder.instance;
    }

    private JdbcRepository() {
        configProperties = BatchConfig.getInstance().getConfigProperties();
        dataSourceName = configProperties.getProperty(DATASOURCE_JNDI_KEY);
        dbUrl = configProperties.getProperty(DB_URL_KEY);

        //if dataSourceName is configured, use dataSourceName;
        //else if dbUrl is specified, use dbUrl;
        //if neither is specified, use default dbUrl;
        if (dataSourceName != null) {
            try {
                dataSource = (DataSource) namingContext.lookup(dataSourceName);
            } catch (NamingException e) {
                throw BatchLogger.LOGGER.failToLookupDataSource(e, dataSourceName);
            }
        } else {
            dbProperties = new Properties();
            if (dbUrl == null) {
                dbUrl = DEFAULT_DB_URL;
                dbUser = configProperties.getProperty(DB_USER_KEY);
                if (dbUser != null) {
                    dbProperties.setProperty("user", dbUser);
                }
                dbPassword = configProperties.getProperty(DB_PASSWORD_KEY);
                if (dbPassword != null) {
                    dbProperties.setProperty("password", dbPassword);
                }
                String s = configProperties.getProperty(DB_PROPERTIES_KEY);
                if (s != null) {
                    String[] ss = s.split(DB_PROPERTY_DELIM);
                    for (String kv : ss) {
                        int equalSign = kv.indexOf('=');
                        if (equalSign > 0) {
                            dbProperties.setProperty(kv.substring(0, equalSign), kv.substring(equalSign + 1));
                        }
                    }
                }
            }
        }
        String sqlFile = dbProperties.getProperty(SQL_FILE_NAME_KEY);
        if (sqlFile == null || sqlFile.isEmpty()) {
            sqlFile = DEFAULT_SQL_FILE;
        }
        InputStream sqlResource = this.getClass().getClassLoader().getResourceAsStream(sqlFile);
        try {
            if (sqlResource == null) {
                throw BatchLogger.LOGGER.failToLoadSqlProperties(null, sqlFile);
            }
            sqls.load(sqlResource);
        } catch (IOException e) {
            throw BatchLogger.LOGGER.failToLoadSqlProperties(e, sqlFile);
        } finally {
            if (sqlResource != null) {
                try {
                    sqlResource.close();
                } catch (IOException e) {
                    BatchLogger.LOGGER.failToClose(e, InputStream.class, sqlResource);
                }
            }
        }
        createTables();
    }

    private void createTables() {
        //first test table existence by running a query
        String getJobInstances = sqls.getProperty(SELECT_ALL_JOB_INSTANCES);
        Connection connection = getConnection();
        PreparedStatement getJobInstancesStatement = null;
        Statement batchDDLStatement = null;
        InputStream ddlResource = null;
        try {
            getJobInstancesStatement = connection.prepareStatement(getJobInstances);
            getJobInstancesStatement.executeQuery();
        } catch (SQLException e) {
            String ddlFile = configProperties.getProperty(DDL_FILE_NAME_KEY);
            String ddlString = null;
            try {
                if (ddlFile == null || ddlFile.isEmpty()) {
                    ddlFile = DEFAULT_DDL_FILE;
                }
                ddlResource = this.getClass().getClassLoader().getResourceAsStream(ddlFile);
                if (ddlResource == null) {
                    throw BatchLogger.LOGGER.failToLoadDDL(ddlFile);
                }
                java.util.Scanner scanner = new java.util.Scanner(ddlResource, "UTF-8").useDelimiter("\\A");
                ddlString = scanner.hasNext() ? scanner.next() : "";
                String[] ddls = ddlString.split(";");


                batchDDLStatement = connection.createStatement();
                for (String ddlEntry : ddls) {
                    batchDDLStatement.addBatch(ddlEntry);
                }
                batchDDLStatement.executeBatch();
            } catch (SQLException sqlException) {
                throw BatchLogger.LOGGER.failToCreateTables(sqlException, ddlFile, ddlString);
            }
            BatchLogger.LOGGER.tableCreated(ddlFile);
            BatchLogger.LOGGER.tableCreated2(ddlString);
        } finally {
            try {
                getJobInstancesStatement.close();
            } catch (Exception e) {
                BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, getJobInstancesStatement);
            }
            try {
                if (batchDDLStatement != null) {
                    batchDDLStatement.close();
                }
            } catch (Exception e) {
                BatchLogger.LOGGER.failToClose(e, Statement.class, batchDDLStatement);
            }
            try {
                if (ddlResource != null) {
                    ddlResource.close();
                }
            } catch (Exception e) {
                BatchLogger.LOGGER.failToClose(e, InputStream.class, ddlResource);
            }
            try {
                connection.close();
            } catch (Exception e) {
                BatchLogger.LOGGER.failToClose(e, Connection.class, connection);
            }
        }
    }

    private Connection getConnection() {
        if (dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw BatchLogger.LOGGER.failToObtainConnection(e, dataSource, dataSourceName);
            }
        } else {
            try {
                return DriverManager.getConnection(dbUrl, dbProperties);
            } catch (SQLException e) {
                throw BatchLogger.LOGGER.failToObtainConnection(e, dbUrl, dbProperties);
            }
        }
    }

    @Override
    void insertJobInstance(JobInstanceImpl jobInstance) {
        String insert = sqls.getProperty(INSERT_JOB_INSTANCE);
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, jobInstance.getJobName());
            preparedStatement.setString(2, jobInstance.getApplicationName());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            jobInstance.setId(resultSet.getLong(1));
            BatchLogger.LOGGER.persisted(jobInstance, jobInstance.getInstanceId());
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToInsert(e, insert);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, preparedStatement);
            }
            try {
                connection.close();
            } catch (SQLException e) {
                BatchLogger.LOGGER.failToClose(e, Connection.class, connection);
            }
        }
    }

    @Override
    void insertJobExecution(JobExecutionImpl jobExecution) {
        String insert = sqls.getProperty(INSERT_JOB_EXECUTION);
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, jobExecution.getJobInstance().getInstanceId());
            preparedStatement.setTimestamp(2, new Timestamp(jobExecution.getCreateTime().getTime()));
            preparedStatement.setTimestamp(3, new Timestamp(jobExecution.getStartTime().getTime()));
            preparedStatement.setString(4, jobExecution.getBatchStatus().name());
            preparedStatement.setString(5, BatchUtil.propertiesToString(jobExecution.getJobParameters()));
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            jobExecution.setId(resultSet.getLong(1));
            BatchLogger.LOGGER.persisted(jobExecution, jobExecution.getExecutionId());
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToInsert(e, insert);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, preparedStatement);
            }
            try {
                connection.close();
            } catch (SQLException e) {
                BatchLogger.LOGGER.failToClose(e, Connection.class, connection);
            }
        }
    }

    @Override
    void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution) {
        String insert = sqls.getProperty(INSERT_STEP_EXECUTION);
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, jobExecution.getExecutionId());
            preparedStatement.setString(2, stepExecution.getStepName());
            preparedStatement.setTimestamp(3, new Timestamp(stepExecution.getStartTime().getTime()));
            preparedStatement.setString(4, stepExecution.getBatchStatus().name());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            stepExecution.setId(resultSet.getLong(1));
            BatchLogger.LOGGER.persisted(stepExecution, stepExecution.getStepExecutionId());
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToInsert(e, insert);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, preparedStatement);
            }
            try {
                connection.close();
            } catch (SQLException e) {
                BatchLogger.LOGGER.failToClose(e, Connection.class, connection);
            }
        }
    }
}
