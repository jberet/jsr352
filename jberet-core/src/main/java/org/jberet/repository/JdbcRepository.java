/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.spi.BatchEnvironment;
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
    private static final String COUNT_JOB_INSTANCES_BY_JOB_NAME = "count-job-instances-by-job-name";
    private static final String SELECT_JOB_INSTANCES_BY_JOB_NAME = "select-job-instances-by-job-name";
    private static final String SELECT_JOB_INSTANCE  = "select-job-instance";
    private static final String INSERT_JOB_INSTANCE  = "insert-job-instance";

    private static final String SELECT_ALL_JOB_EXECUTIONS = "select-all-job-executions";
    private static final String SELECT_JOB_EXECUTIONS_BY_JOB_INSTANCE_ID = "select-job-executions-by-job-instance-id";
    private static final String SELECT_JOB_EXECUTION = "select-job-execution";
    private static final String INSERT_JOB_EXECUTION = "insert-job-execution";

    private static final String SELECT_ALL_STEP_EXECUTIONS = "select-all-step-executions";
    private static final String SELECT_STEP_EXECUTIONS_BY_JOB_EXECUTION_ID = "select-step-executions-by-job-execution-id";
    private static final String SELECT_STEP_EXECUTION = "select-step-execution";
    private static final String INSERT_STEP_EXECUTION = "insert-step-execution";

    /**
     * A class to hold all table names and column names.  Commented-out column names are already defined in other tables,
     * and are kept there as comment line for completeness.
     */
    private static class TableColumn {
        //table name
        private static final String JOB_INSTANCE = "JOB_INSTANCE";
        //column names
        //private static final String JOBINSTANCEID = "JOBINSTANCEID";
        private static final String JOBNAME = "JOBNAME";
        private static final String APPLICATIONNAME = "APPLICATIONNAME";
        
        //table name
        private static final String JOB_EXECUTION = "JOB_EXECUTION";
        //column names
        //private static final String JOBEXECUTIONID = "JOBEXECUTIONID";
        private static final String JOBINSTANCEID = "JOBINSTANCEID";
        private static final String CREATETIME = "CREATETIME";
        //private static final String STARTTIME = "STARTTIME";
        //private static final String ENDTIME = "ENDTIME";
        private static final String LASTUPDATEDTIME = "LASTUPDATEDTIME";
        //private static final String BATCHSTATUS = "BATCHSTATUS";
        //private static final String EXITSTATUS = "EXITSTATUS";
        private static final String JOBPARAMETERS = "JOBPARAMETERS";
        
        //table name
        private static final String STEP_EXECUTION = "STEP_EXECUTION";
        //column names
        private static final String STEPEXECUTIONID = "STEPEXECUTIONID";
        private static final String JOBEXECUTIONID = "JOBEXECUTIONID";
        private static final String STEPNAME = "STEPNAME";
        private static final String STARTTIME = "STARTTIME";
        private static final String ENDTIME = "ENDTIME";
        private static final String BATCHSTATUS = "BATCHSTATUS";
        private static final String EXITSTATUS = "EXITSTATUS";
        private static final String PERSISTENTUSERDATA = "PERSISTENTUSERDATA";
        private static final String READCOUNT = "READCOUNT";
        private static final String WRITECOUNT = "WRITECOUNT";
        private static final String COMMITCOUNT = "COMMITCOUNT";
        private static final String ROLLBACKCOUNT = "ROLLBACKCOUNT";
        private static final String READSKIPCOUNT = "READSKIPCOUNT";
        private static final String PROCESSSKIPCOUNT = "PROCESSSKIPCOUNT";
        private static final String FILTERCOUNT = "FILTERCOUNT";
        private static final String WRITESKIPCOUNT = "WRITESKIPCOUNT";
        
        private TableColumn() {}
    }

    private static volatile JdbcRepository instance;
    private Properties configProperties;
    private Context namingContext;
    private String dataSourceName;
    private DataSource dataSource;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Properties dbProperties;
    private final Properties sqls = new Properties();

    static JdbcRepository getInstance(final BatchEnvironment batchEnvironment) {
        JdbcRepository result = instance;
        if(result == null) {
            synchronized (JdbcRepository.class) {
                result = instance;
                if(result == null) {
                    instance = result = new JdbcRepository(batchEnvironment);
                }
            }
        }
        return result;
    }

    private JdbcRepository(final BatchEnvironment batchEnvironment) {
        configProperties = batchEnvironment.getBatchConfigurationProperties();
        dataSourceName = configProperties.getProperty(DATASOURCE_JNDI_KEY);
        dbUrl = configProperties.getProperty(DB_URL_KEY);

        //if dataSourceName is configured, use dataSourceName;
        //else if dbUrl is specified, use dbUrl;
        //if neither is specified, use default dbUrl;
        if (dataSourceName != null) {
            try {
                dataSource = InitialContext.doLookup(dataSourceName);
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
                final String s = configProperties.getProperty(DB_PROPERTIES_KEY);
                if (s != null) {
                    final String[] ss = s.split(DB_PROPERTY_DELIM);
                    for (final String kv : ss) {
                        final int equalSign = kv.indexOf('=');
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
        final InputStream sqlResource = this.getClass().getClassLoader().getResourceAsStream(sqlFile);
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
        final String getJobInstances = sqls.getProperty(SELECT_ALL_JOB_INSTANCES);
        final Connection connection = getConnection();
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
                final java.util.Scanner scanner = new java.util.Scanner(ddlResource, "UTF-8").useDelimiter("\\A");
                ddlString = scanner.hasNext() ? scanner.next() : "";
                final String[] ddls = ddlString.split(";");


                batchDDLStatement = connection.createStatement();
                for (final String ddlEntry : ddls) {
                    batchDDLStatement.addBatch(ddlEntry);
                }
                batchDDLStatement.executeBatch();
            } catch (SQLException sqlException) {
                throw BatchLogger.LOGGER.failToCreateTables(sqlException, ddlFile, ddlString);
            }
            BatchLogger.LOGGER.tableCreated(ddlFile);
            BatchLogger.LOGGER.tableCreated2(ddlString);
        } finally {
            close(connection, getJobInstancesStatement, batchDDLStatement);
            try {
                if (ddlResource != null) {
                    ddlResource.close();
                }
            } catch (Exception e) {
                BatchLogger.LOGGER.failToClose(e, InputStream.class, ddlResource);
            }
        }
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) {
        //check cache first, if not found, then retrieve from database
        List<StepExecution> stepExecutions = super.getStepExecutions(jobExecutionId);
        if (stepExecutions.isEmpty()) {
            stepExecutions = selectStepExecutions(jobExecutionId);
        }
        return stepExecutions;
    }


    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        final String insert = sqls.getProperty(INSERT_JOB_INSTANCE);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, jobInstance.getJobName());
            preparedStatement.setString(2, jobInstance.getApplicationName());
            preparedStatement.executeUpdate();
            final ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            jobInstance.setId(resultSet.getLong(1));
            BatchLogger.LOGGER.persisted(jobInstance, jobInstance.getInstanceId());
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null);
        }
    }

    List<JobInstance> selectJobInstances(final String jobName) {
        final String select = (jobName == null) ? sqls.getProperty(SELECT_ALL_JOB_INSTANCES) :
                sqls.getProperty(SELECT_JOB_INSTANCES_BY_JOB_NAME);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        final List<JobInstance> result = new ArrayList<JobInstance>();
        try {
            preparedStatement = connection.prepareStatement(select);
            if (jobName != null) {
                preparedStatement.setString(1, jobName);
            }
            final ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final long i = rs.getLong(TableColumn.JOBINSTANCEID);
                JobInstance jobInstance1 = jobInstances.get(i);
                if (jobInstance1 == null) {
                    final String appName = rs.getString(TableColumn.APPLICATIONNAME);
                    if (jobName == null) {
                        final String goodJobName = rs.getString(TableColumn.JOBNAME);
                        jobInstance1 = new JobInstanceImpl(getJob(goodJobName), new ApplicationAndJobName(appName, goodJobName));
                    } else {
                        jobInstance1 = new JobInstanceImpl(getJob(jobName), new ApplicationAndJobName(appName, jobName));
                    }
                    jobInstances.put(i, jobInstance1);
                }
                //this job instance is already in the cache, so get it from the cache
                result.add(jobInstance1);
            }
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return result;
    }

    JobInstance selectJobInstance(final long jobInstanceId) {
        final String select = sqls.getProperty(SELECT_JOB_INSTANCE);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        JobInstance result = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, jobInstanceId);
            final ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                result = jobInstances.get(jobInstanceId);
                if (result == null) {
                    final String appName = rs.getString(TableColumn.APPLICATIONNAME);
                    final String goodJobName = rs.getString(TableColumn.JOBNAME);
                    result = new JobInstanceImpl(getJob(goodJobName), new ApplicationAndJobName(appName, goodJobName));
                    jobInstances.put(jobInstanceId, result);
                }
                break;
            }
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return result;
    }

    int countJobInstance(final String jobName) {
        final String select = sqls.getProperty(COUNT_JOB_INSTANCES_BY_JOB_NAME);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        int count = 0;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setString(1, jobName);
            final ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                count = rs.getInt(1);
                break;
            }
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return count;
    }

    @Override
    void insertJobExecution(final JobExecutionImpl jobExecution) {
        final String insert = sqls.getProperty(INSERT_JOB_EXECUTION);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, jobExecution.getJobInstance().getInstanceId());
            preparedStatement.setTimestamp(2, new Timestamp(jobExecution.getCreateTime().getTime()));
            preparedStatement.setTimestamp(3, new Timestamp(jobExecution.getStartTime().getTime()));
            preparedStatement.setString(4, jobExecution.getBatchStatus().name());
            preparedStatement.setString(5, BatchUtil.propertiesToString(jobExecution.getJobParameters()));
            preparedStatement.executeUpdate();
            final ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            jobExecution.setId(resultSet.getLong(1));
            BatchLogger.LOGGER.persisted(jobExecution, jobExecution.getExecutionId());
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null);
        }
    }

    JobExecution selectJobExecution(final long jobExecutionId) {
        final String select = sqls.getProperty(SELECT_JOB_EXECUTION);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        JobExecution result = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, jobExecutionId);
            final ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                result = jobExecutions.get(jobExecutionId);
                if (result == null) {
                    final long jobInstanceId = rs.getLong(TableColumn.JOBINSTANCEID);
                    result = new JobExecutionImpl((JobInstanceImpl) getJobInstance(jobInstanceId), null);
                    jobExecutions.put(jobExecutionId, result);
                }
                break;
            }
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return result;
    }

    List<JobExecution> selectJobExecutions(final Long jobInstanceId) {
        final String select = (jobInstanceId == null) ? sqls.getProperty(SELECT_ALL_JOB_EXECUTIONS) :
                sqls.getProperty(SELECT_JOB_EXECUTIONS_BY_JOB_INSTANCE_ID);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        final List<JobExecution> result = new ArrayList<JobExecution>();
        try {
            preparedStatement = connection.prepareStatement(select);
            if (jobInstanceId != null) {
                preparedStatement.setLong(1, jobInstanceId);
            }
            final ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final long i = rs.getLong(TableColumn.JOBEXECUTIONID);
                JobExecution jobExecution1 = jobExecutions.get(i);
                if (jobExecution1 == null) {
                    if (jobInstanceId == null) {
                        final long jobInstanceId1 = rs.getLong(TableColumn.JOBINSTANCEID);
                        jobExecution1 = new JobExecutionImpl((JobInstanceImpl) getJobInstance(jobInstanceId1), null);
                    } else {
                        jobExecution1 = new JobExecutionImpl((JobInstanceImpl) getJobInstance(jobInstanceId), null);
                    }
                    jobExecutions.put(i, jobExecution1);
                }
                //this job instance is already in the cache, so get it from the cache
                result.add(jobExecution1);
            }
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return result;
    }

    @Override
    void insertStepExecution(final StepExecutionImpl stepExecution, final JobExecutionImpl jobExecution) {
        final String insert = sqls.getProperty(INSERT_STEP_EXECUTION);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, jobExecution.getExecutionId());
            preparedStatement.setString(2, stepExecution.getStepName());
            preparedStatement.setTimestamp(3, new Timestamp(stepExecution.getStartTime().getTime()));
            preparedStatement.setString(4, stepExecution.getBatchStatus().name());
            preparedStatement.executeUpdate();
            final ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            stepExecution.setId(resultSet.getLong(1));
            BatchLogger.LOGGER.persisted(stepExecution, stepExecution.getStepExecutionId());
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null);
        }
    }

    StepExecution selectStepExecution(final long stepExecutionId) {
        final String select = sqls.getProperty(SELECT_STEP_EXECUTION);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        List<StepExecution> result = new ArrayList<StepExecution>();
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, stepExecutionId);
            createStepExecutionsFromResultSet(preparedStatement.executeQuery(), result);
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return result.get(0);
    }

    /**
     * Retrieves a list of StepExecution from database by JobExecution id.  This method does not check the cache, so it
     * should only be called after the cache has been searched without a match.
     * @param jobExecutionId    if null, retrieves all StepExecutions; otherwise, retrieves all StepExecutions belongs to the JobExecution id
     * @return a list of StepExecutions
     */
    List<StepExecution> selectStepExecutions(final Long jobExecutionId) {
        final String select = (jobExecutionId == null) ? sqls.getProperty(SELECT_ALL_STEP_EXECUTIONS) :
                sqls.getProperty(SELECT_STEP_EXECUTIONS_BY_JOB_EXECUTION_ID);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        final List<StepExecution> result = new ArrayList<StepExecution>();
        try {
            preparedStatement = connection.prepareStatement(select);
            if (jobExecutionId != null) {
                preparedStatement.setLong(1, jobExecutionId);
            }
            createStepExecutionsFromResultSet(preparedStatement.executeQuery(), result);
        } catch (SQLException e) {
            throw BatchLogger.LOGGER.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null);
        }
        return result;
    }

    private void createStepExecutionsFromResultSet(final ResultSet rs, final List<StepExecution> result) throws SQLException {
        while (rs.next()) {
            final StepExecutionImpl e = new StepExecutionImpl(
                    rs.getLong(TableColumn.STEPEXECUTIONID),
                    rs.getString(TableColumn.STEPNAME),
                    rs.getLong(TableColumn.STARTTIME),
                    rs.getLong(TableColumn.ENDTIME),
                    rs.getString(TableColumn.BATCHSTATUS),
                    rs.getString(TableColumn.EXITSTATUS),
                    rs.getObject(TableColumn.PERSISTENTUSERDATA),
                    rs.getInt(TableColumn.READCOUNT),
                    rs.getInt(TableColumn.WRITECOUNT),
                    rs.getInt(TableColumn.COMMITCOUNT),
                    rs.getInt(TableColumn.ROLLBACKCOUNT),
                    rs.getInt(TableColumn.READSKIPCOUNT),
                    rs.getInt(TableColumn.PROCESSSKIPCOUNT),
                    rs.getInt(TableColumn.FILTERCOUNT),
                    rs.getInt(TableColumn.WRITESKIPCOUNT)
            );
            result.add(e);
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

    private void close(final Connection conn, final Statement stmt1, final Statement stmt2) {
        try {
            if (stmt1 != null) {
                stmt1.close();
            }
        } catch (SQLException e) {
            BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, stmt1);
        }

        try {
            if (stmt2 != null) {
                stmt2.close();
            }
        } catch (SQLException e) {
            BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, stmt2);
        }

        try {
            conn.close();
        } catch (SQLException e) {
            BatchLogger.LOGGER.failToClose(e, Connection.class, conn);
        }
    }

}
