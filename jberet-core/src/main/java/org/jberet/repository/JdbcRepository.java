/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

public final class JdbcRepository extends AbstractPersistentRepository {
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
    //private static final String DEFAULT_DATASOURCE = "java:jboss/datasources/ExampleDS";
    //    private static final String DEFAULT_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DEFAULT_DB_URL = "jdbc:h2:~/jberet-repo";
    private static final String DEFAULT_SQL_FILE = "sql/jberet-sql.properties";
    private static final String DEFAULT_DDL_FILE = "sql/jberet.ddl";

    //keys used in *.sql files
    private static final String SELECT_ALL_JOB_INSTANCES = "select-all-job-instances";
    private static final String COUNT_JOB_INSTANCES_BY_JOB_NAME = "count-job-instances-by-job-name";
    private static final String SELECT_JOB_INSTANCES_BY_JOB_NAME = "select-job-instances-by-job-name";
    private static final String SELECT_JOB_INSTANCE = "select-job-instance";
    private static final String INSERT_JOB_INSTANCE = "insert-job-instance";

    private static final String SELECT_ALL_JOB_EXECUTIONS = "select-all-job-executions";
    private static final String SELECT_JOB_EXECUTIONS_BY_JOB_INSTANCE_ID = "select-job-executions-by-job-instance-id";
    private static final String SELECT_RUNNING_JOB_EXECUTIONS_BY_JOB_NAME = "select-running-job-executions-by-job-name";
    private static final String SELECT_JOB_EXECUTION = "select-job-execution";
    private static final String INSERT_JOB_EXECUTION = "insert-job-execution";
    private static final String UPDATE_JOB_EXECUTION = "update-job-execution";
    private static final String UPDATE_JOB_EXECUTION_AND_PARAMETERS = "update-job-execution-and-parameters";
    private static final String UPDATE_JOB_EXECUTION_PARTIAL = "update-job-execution-partial";

    private static final String SELECT_ALL_STEP_EXECUTIONS = "select-all-step-executions";
    private static final String SELECT_STEP_EXECUTIONS_BY_JOB_EXECUTION_ID = "select-step-executions-by-job-execution-id";
    private static final String SELECT_STEP_EXECUTION = "select-step-execution";
    private static final String INSERT_STEP_EXECUTION = "insert-step-execution";
    private static final String UPDATE_STEP_EXECUTION = "update-step-execution";

    private static final String FIND_ORIGINAL_STEP_EXECUTION = "find-original-step-execution";
    private static final String COUNT_STEP_EXECUTIONS_BY_JOB_INSTANCE_ID = "count-step-executions-by-job-instance-id";

    //private static final String SELECT_ALL_PARTITION_EXECUTIONS = "select-all-partition-executions";
    private static final String COUNT_PARTITION_EXECUTIONS = "count-partition-executions";
    private static final String SELECT_PARTITION_EXECUTIONS_BY_STEP_EXECUTION_ID = "select-partition-executions-by-step-execution-id";
    private static final String INSERT_PARTITION_EXECUTION = "insert-partition-execution";
    private static final String UPDATE_PARTITION_EXECUTION = "update-partition-execution";

    private final DataSource dataSource;
    private final String dbUrl;
    private final String userDefinedDdlFile;
    private final Properties dbProperties;
    private final Properties sqls = new Properties();
    private boolean isOracle;
    private int[] idIndexInOracle;

    public static JdbcRepository create(final Properties configProperties) {
        return new JdbcRepository(configProperties);
    }

    public JdbcRepository(final Properties configProperties) {
        String dataSourceName = configProperties.getProperty(DATASOURCE_JNDI_KEY);
        dbProperties = new Properties();
        userDefinedDdlFile = configProperties.getProperty(DDL_FILE_NAME_KEY);

        //if dataSourceName is configured, use dataSourceName;
        //else if dbUrl is specified, use dbUrl;
        //if neither is specified, use default dbUrl;
        if (dataSourceName != null) {
            dataSourceName = dataSourceName.trim();
        }
        if (dataSourceName != null && !dataSourceName.isEmpty()) {
            dbUrl = null;
            try {
                dataSource = InitialContext.doLookup(dataSourceName);
            } catch (final NamingException e) {
                throw BatchMessages.MESSAGES.failToLookupDataSource(e, dataSourceName);
            }
        } else {
            String dbUrl = configProperties.getProperty(DB_URL_KEY);
            dataSource = null;
            if (dbUrl != null) {
                dbUrl = dbUrl.trim();
            }
            if (dbUrl == null || dbUrl.isEmpty()) {
                dbUrl = DEFAULT_DB_URL;
            }
            this.dbUrl = dbUrl;
            final String dbUser = configProperties.getProperty(DB_USER_KEY);
            if (dbUser != null) {
                dbProperties.setProperty("user", dbUser.trim());
            }
            final String dbPassword = configProperties.getProperty(DB_PASSWORD_KEY);
            if (dbPassword != null) {
                dbProperties.setProperty("password", dbPassword.trim());
            }
            final String s = configProperties.getProperty(DB_PROPERTIES_KEY);
            if (s != null) {
                final String[] ss = s.trim().split(DB_PROPERTY_DELIM);
                for (final String kv : ss) {
                    final int equalSign = kv.indexOf('=');
                    if (equalSign > 0) {
                        dbProperties.setProperty(kv.substring(0, equalSign), kv.substring(equalSign + 1));
                    }
                }
            }
        }
        createTables(configProperties);
    }

    /**
     * Creates a new JDBC job repository.
     *
     * @param dataSource the data source used to connect to the database
     */
    public JdbcRepository(final DataSource dataSource) {
        this(dataSource, new Properties());
    }

    /**
     * Creates a new JDBC job repository.
     *
     * @param dataSource       the data source used to connect to the database
     * @param configProperties the configuration properties to use
     */
    public JdbcRepository(final DataSource dataSource, final Properties configProperties) {
        if (dataSource == null) {
            throw BatchMessages.MESSAGES.nullVar("dataSource");
        }
        if (configProperties == null) {
            throw BatchMessages.MESSAGES.nullVar("configProperties");
        }
        dbProperties = new Properties();
        userDefinedDdlFile = configProperties.getProperty(DDL_FILE_NAME_KEY);
        this.dataSource = dataSource;
        dbUrl = null;
        String sqlFile = DEFAULT_SQL_FILE;
        final InputStream sqlResource = getClassLoader().getResourceAsStream(sqlFile);
        try {
            if (sqlResource == null) {
                throw BatchMessages.MESSAGES.failToLoadSqlProperties(null, sqlFile);
            }
            sqls.load(sqlResource);
        } catch (final IOException e) {
            throw BatchMessages.MESSAGES.failToLoadSqlProperties(e, sqlFile);
        } finally {
            if (sqlResource != null) {
                try {
                    sqlResource.close();
                } catch (final IOException e) {
                    BatchLogger.LOGGER.failToClose(e, InputStream.class, sqlResource);
                }
            }
        }
        createTables(configProperties);
    }

    private void createTables(final Properties configProperties) {
        String sqlFile = configProperties.getProperty(SQL_FILE_NAME_KEY);
        if (sqlFile != null) {
            sqlFile = sqlFile.trim();
        }
        if (sqlFile == null || sqlFile.isEmpty()) {
            sqlFile = DEFAULT_SQL_FILE;
        }
        final InputStream sqlResource = getClassLoader().getResourceAsStream(sqlFile);
        try {
            if (sqlResource == null) {
                throw BatchMessages.MESSAGES.failToLoadSqlProperties(null, sqlFile);
            }
            sqls.load(sqlResource);
        } catch (final IOException e) {
            throw BatchMessages.MESSAGES.failToLoadSqlProperties(e, sqlFile);
        } finally {
            if (sqlResource != null) {
                try {
                    sqlResource.close();
                } catch (final IOException e) {
                    BatchLogger.LOGGER.failToClose(e, InputStream.class, sqlResource);
                }
            }
        }
        //first test table existence by running a query against the last table in the ddl entry list
        final String countPartitionExecutions = sqls.getProperty(COUNT_PARTITION_EXECUTIONS);
        Connection connection1 = getConnection();
        ResultSet rs = null;
        PreparedStatement countPartitionExecutionStatement = null;
        PreparedStatement countJobInstancesStatement = null;
        InputStream ddlResource = null;

        String databaseProductName = "";
        try {
            databaseProductName = connection1.getMetaData().getDatabaseProductName().trim();
        } catch (final SQLException e) {
            BatchLogger.LOGGER.failToGetDatabaseProductName(e, connection1);
            close(connection1, null, null, null);
            connection1 = getConnection();
        } catch (final Exception e) {
            BatchLogger.LOGGER.failToGetDatabaseProductName(e, connection1);
        }
        if (databaseProductName.startsWith("Oracle")) {
            isOracle = true;
            idIndexInOracle = new int[]{1};
        }

        try {
            countPartitionExecutionStatement = connection1.prepareStatement(countPartitionExecutions);
            rs = countPartitionExecutionStatement.executeQuery();
        } catch (final SQLException e) {
            final String ddlFile = getDDLLocation(databaseProductName);
            ddlResource = getClassLoader().getResourceAsStream(ddlFile);
            if (ddlResource == null) {
                throw BatchMessages.MESSAGES.failToLoadDDL(ddlFile);
            }
            final java.util.Scanner scanner = new java.util.Scanner(ddlResource).useDelimiter("!!");
            Connection connection2 = null;
            Statement batchDDLStatement = null;
            try {
                connection2 = getConnection();
                batchDDLStatement = connection2.createStatement();
                while (scanner.hasNext()) {
                    final String ddlEntry = scanner.next().trim();
                    if (!ddlEntry.isEmpty()) {
                        batchDDLStatement.addBatch(ddlEntry);
                        BatchLogger.LOGGER.addDDLEntry(ddlEntry);
                    }
                }
                scanner.close();
                batchDDLStatement.executeBatch();
                BatchLogger.LOGGER.tableCreated(ddlFile);
            } catch (final Exception e1) {
                //check if the tables have just been created by another concurrent client in the interim
                try {
                    final String countJobInstances = sqls.getProperty(COUNT_JOB_INSTANCES_BY_JOB_NAME);
                    countJobInstancesStatement = connection1.prepareStatement(countJobInstances);
                    countJobInstancesStatement.setString(1, "A");
                    rs = countJobInstancesStatement.executeQuery();
                    BatchLogger.LOGGER.tracef(
                    "This invocation needed to create tables since they didn't exit, but failed to create because they've been created by another concurrent invocation, so ignore the exception and return normally: %s", e1);
                } catch (final SQLException sqle) {
                    //still cannot access the table, so fail it
                    throw BatchMessages.MESSAGES.failToCreateTables(e1, databaseProductName, ddlFile);
                }
            } finally {
                close(connection2, batchDDLStatement, null, null);
            }
        } finally {
            close(connection1, countPartitionExecutionStatement, countJobInstancesStatement, rs);
            try {
                if (ddlResource != null) {
                    ddlResource.close();
                }
            } catch (final Exception e) {
                BatchLogger.LOGGER.failToClose(e, InputStream.class, ddlResource);
            }
        }
    }

    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        final String insert = sqls.getProperty(INSERT_JOB_INSTANCE);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = isOracle ? connection.prepareStatement(insert, idIndexInOracle) :
                    connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, jobInstance.getJobName());
            preparedStatement.setString(2, jobInstance.getApplicationName());
            preparedStatement.executeUpdate();
            rs = preparedStatement.getGeneratedKeys();
            rs.next();
            jobInstance.setId(rs.getLong(1));
            BatchLogger.LOGGER.persisted(jobInstance, jobInstance.getInstanceId());
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        final String select = (jobName == null) ? sqls.getProperty(SELECT_ALL_JOB_INSTANCES) :
                sqls.getProperty(SELECT_JOB_INSTANCES_BY_JOB_NAME);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        final List<JobInstance> result = new ArrayList<JobInstance>();
        try {
            preparedStatement = connection.prepareStatement(select);
            if (jobName != null) {
                preparedStatement.setString(1, jobName);
            }
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final long i = rs.getLong(TableColumns.JOBINSTANCEID);
                final SoftReference<JobInstanceImpl, Long> ref = jobInstances.get(i);
                JobInstanceImpl jobInstance1 = (ref != null) ? ref.get() : null;
                if (jobInstance1 == null) {
                    final String appName = rs.getString(TableColumns.APPLICATIONNAME);
                    if (jobName == null) {
                        final String goodJobName = rs.getString(TableColumns.JOBNAME);
                        jobInstance1 = new JobInstanceImpl(getJob(new ApplicationAndJobName(appName, goodJobName)), appName, goodJobName);
                    } else {
                        jobInstance1 = new JobInstanceImpl(getJob(new ApplicationAndJobName(appName, jobName)), appName, jobName);
                    }
                    jobInstance1.setId(i);
                    jobInstances.put(i, new SoftReference<JobInstanceImpl, Long>(jobInstance1, jobInstanceReferenceQueue, i));
                }
                //this job instance is already in the cache, so get it from the cache
                result.add(jobInstance1);
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    @Override
    public JobInstanceImpl getJobInstance(final long jobInstanceId) {
        JobInstanceImpl result = super.getJobInstance(jobInstanceId);
        if (result != null) {
            return result;
        }

        final String select = sqls.getProperty(SELECT_JOB_INSTANCE);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, jobInstanceId);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final SoftReference<JobInstanceImpl, Long> jobInstanceSoftReference = jobInstances.get(jobInstanceId);
                result = jobInstanceSoftReference != null ? jobInstanceSoftReference.get() : null;
                if (result == null) {
                    final String appName = rs.getString(TableColumns.APPLICATIONNAME);
                    final String goodJobName = rs.getString(TableColumns.JOBNAME);
                    result = new JobInstanceImpl(getJob(new ApplicationAndJobName(appName, goodJobName)), appName, goodJobName);
                    result.setId(jobInstanceId);
                    jobInstances.put(jobInstanceId,
                            new SoftReference<JobInstanceImpl, Long>(result, jobInstanceReferenceQueue, jobInstanceId));
                }
                break;
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        final String select = sqls.getProperty(COUNT_JOB_INSTANCES_BY_JOB_NAME);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        int count = 0;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setString(1, jobName);
            rs = preparedStatement.executeQuery();

            while (rs.next()) {
                count = rs.getInt(1);
                break;
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return count;
    }

    @Override
    void insertJobExecution(final JobExecutionImpl jobExecution) {
        final String insert = sqls.getProperty(INSERT_JOB_EXECUTION);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = isOracle ? connection.prepareStatement(insert, idIndexInOracle) :
                    connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, jobExecution.getJobInstance().getInstanceId());
            preparedStatement.setTimestamp(2, createTimestamp(jobExecution.getCreateTime()));
            preparedStatement.setString(3, jobExecution.getBatchStatus().name());
            preparedStatement.setString(4, BatchUtil.propertiesToString(jobExecution.getJobParameters()));
            preparedStatement.executeUpdate();
            rs = preparedStatement.getGeneratedKeys();
            rs.next();
            jobExecution.setId(rs.getLong(1));
            BatchLogger.LOGGER.persisted(jobExecution, jobExecution.getExecutionId());
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate, final boolean saveJobParameters) {
        super.updateJobExecution(jobExecution, fullUpdate, saveJobParameters);
        final String update;
        if (fullUpdate) {
            if (saveJobParameters) {
                update = sqls.getProperty(UPDATE_JOB_EXECUTION_AND_PARAMETERS);
            } else {
                update = sqls.getProperty(UPDATE_JOB_EXECUTION);
            }
        } else {
            update = sqls.getProperty(UPDATE_JOB_EXECUTION_PARTIAL);
        }

        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(update);

            if (fullUpdate) {
                preparedStatement.setTimestamp(1, createTimestamp(jobExecution.getEndTime()));
                preparedStatement.setTimestamp(2, createTimestamp(jobExecution.getLastUpdatedTime()));
                preparedStatement.setString(3, jobExecution.getBatchStatus().name());
                preparedStatement.setString(4, jobExecution.getExitStatus());
                preparedStatement.setString(5, jobExecution.combineRestartPositionAndUser());

                if (saveJobParameters) {
                    preparedStatement.setString(6, BatchUtil.propertiesToString(jobExecution.getJobParameters()));  //job parameters
                    preparedStatement.setLong(7, jobExecution.getExecutionId());  //where clause
                } else {
                    preparedStatement.setLong(6, jobExecution.getExecutionId());  //where clause
                }
            } else {
                preparedStatement.setTimestamp(1, createTimestamp(jobExecution.getLastUpdatedTime()));
                preparedStatement.setTimestamp(2, createTimestamp(jobExecution.getStartTime()));
                preparedStatement.setString(3, jobExecution.getBatchStatus().name());
                preparedStatement.setLong(4, jobExecution.getExecutionId());  //where clause
            }
            preparedStatement.executeUpdate();
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, update);
        } finally {
            close(connection, preparedStatement, null, null);
        }
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        JobExecutionImpl result = super.getJobExecution(jobExecutionId);
        if (result != null && !isExecutionStale(result)) {
            return result;
        }
        final String select = sqls.getProperty(SELECT_JOB_EXECUTION);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, jobExecutionId);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final SoftReference<JobExecutionImpl, Long> ref = jobExecutions.get(jobExecutionId);
                result = (ref != null) ? ref.get() : null;
                final long jobInstanceId = rs.getLong(TableColumns.JOBINSTANCEID);
                if (result == null) {
                    result = new JobExecutionImpl(getJobInstance(jobInstanceId),
                            jobExecutionId,
                            BatchUtil.stringToProperties(rs.getString(TableColumns.JOBPARAMETERS)),
                            rs.getTimestamp(TableColumns.CREATETIME),
                            rs.getTimestamp(TableColumns.STARTTIME),
                            rs.getTimestamp(TableColumns.ENDTIME),
                            rs.getTimestamp(TableColumns.LASTUPDATEDTIME),
                            rs.getString(TableColumns.BATCHSTATUS),
                            rs.getString(TableColumns.EXITSTATUS),
                            rs.getString(TableColumns.RESTARTPOSITION));
                    jobExecutions.put(jobExecutionId,
                            new SoftReference<JobExecutionImpl, Long>(result, jobExecutionReferenceQueue, jobExecutionId));
                } else {
                    if (result.getEndTime() == null && rs.getTimestamp(TableColumns.ENDTIME) != null) {
                        final Properties jobParameters1 = BatchUtil.stringToProperties(rs.getString(TableColumns.JOBPARAMETERS));
                        result = new JobExecutionImpl(getJobInstance(jobInstanceId),
                              jobExecutionId,
                              BatchUtil.stringToProperties(rs.getString(TableColumns.JOBPARAMETERS)),
                              rs.getTimestamp(TableColumns.CREATETIME),
                              rs.getTimestamp(TableColumns.STARTTIME),
                              rs.getTimestamp(TableColumns.ENDTIME),
                              rs.getTimestamp(TableColumns.LASTUPDATEDTIME),
                              rs.getString(TableColumns.BATCHSTATUS),
                              rs.getString(TableColumns.EXITSTATUS),
                              rs.getString(TableColumns.RESTARTPOSITION));
                        jobExecutions.replace(jobExecutionId,
                                new SoftReference<JobExecutionImpl, Long>(result, jobExecutionReferenceQueue, jobExecutionId));
                    }
                }
                break;
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        final String select;
        long jobInstanceId = 0;
        if (jobInstance == null) {
            select = sqls.getProperty(SELECT_ALL_JOB_EXECUTIONS);
        } else {
            select = sqls.getProperty(SELECT_JOB_EXECUTIONS_BY_JOB_INSTANCE_ID);
            jobInstanceId = jobInstance.getInstanceId();
        }

        final List<JobExecution> result = new ArrayList<JobExecution>();
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            if (jobInstance != null) {
                preparedStatement.setLong(1, jobInstanceId);
            }
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final long executionId = rs.getLong(TableColumns.JOBEXECUTIONID);
                final SoftReference<JobExecutionImpl, Long> ref = jobExecutions.get(executionId);
                JobExecutionImpl jobExecution1 = (ref != null) ? ref.get() : null;
                if (jobExecution1 == null) {
                    if (jobInstanceId == 0) {
                        jobInstanceId = rs.getLong(TableColumns.JOBINSTANCEID);
                    }
                    final Properties jobParameters1 = BatchUtil.stringToProperties(rs.getString(TableColumns.JOBPARAMETERS));
                    jobExecution1 =
                            new JobExecutionImpl(getJobInstance(jobInstanceId), executionId, jobParameters1,
                                    rs.getTimestamp(TableColumns.CREATETIME), rs.getTimestamp(TableColumns.STARTTIME),
                                    rs.getTimestamp(TableColumns.ENDTIME), rs.getTimestamp(TableColumns.LASTUPDATEDTIME),
                                    rs.getString(TableColumns.BATCHSTATUS), rs.getString(TableColumns.EXITSTATUS),
                                    rs.getString(TableColumns.RESTARTPOSITION));

                    jobExecutions.put(executionId,
                            new SoftReference<JobExecutionImpl, Long>(jobExecution1, jobExecutionReferenceQueue, executionId));
                } else {
                    if (jobExecution1.getEndTime() == null && rs.getTimestamp(TableColumns.ENDTIME) != null) {
                        final Properties jobParameters1 = BatchUtil.stringToProperties(rs.getString(TableColumns.JOBPARAMETERS));
                        jobExecution1 =
                                new JobExecutionImpl(getJobInstance(jobInstanceId), executionId, jobParameters1,
                                      rs.getTimestamp(TableColumns.CREATETIME), rs.getTimestamp(TableColumns.STARTTIME),
                                      rs.getTimestamp(TableColumns.ENDTIME), rs.getTimestamp(TableColumns.LASTUPDATEDTIME),
                                      rs.getString(TableColumns.BATCHSTATUS), rs.getString(TableColumns.EXITSTATUS),
                                      rs.getString(TableColumns.RESTARTPOSITION));
                        jobExecutions.replace(executionId,
                                new SoftReference<JobExecutionImpl, Long>(jobExecution1, jobExecutionReferenceQueue, executionId));
                    }
                }
                // jobExecution1 is either got from the cache, or created, now add it to the result list
                result.add(jobExecution1);
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    private boolean isExecutionStale(final JobExecutionImpl jobExecution) {
        final BatchStatus jobStatus = jobExecution.getBatchStatus();
        if (jobStatus.equals(BatchStatus.COMPLETED) ||
            jobStatus.equals(BatchStatus.FAILED) ||
            jobStatus.equals(BatchStatus.STOPPED) ||
            jobStatus.equals(BatchStatus.ABANDONED) || jobExecution.getStepExecutions().size() >= 1) {
            return false;
        }

        return true;
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        final List<Long> result = new ArrayList<Long>();
        final String select = sqls.getProperty(SELECT_RUNNING_JOB_EXECUTIONS_BY_JOB_NAME);

        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setString(1, jobName);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final long i = rs.getLong(1);
                result.add(i);
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    @Override
    void insertStepExecution(final StepExecutionImpl stepExecution, final JobExecutionImpl jobExecution) {
        final String insert = sqls.getProperty(INSERT_STEP_EXECUTION);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = isOracle ? connection.prepareStatement(insert, idIndexInOracle) :
                    connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, jobExecution.getExecutionId());
            preparedStatement.setString(2, stepExecution.getStepName());
            preparedStatement.setTimestamp(3, new Timestamp(stepExecution.getStartTime().getTime()));
            preparedStatement.setString(4, stepExecution.getBatchStatus().name());
            preparedStatement.executeUpdate();
            rs = preparedStatement.getGeneratedKeys();
            rs.next();
            stepExecution.setId(rs.getLong(1));
            BatchLogger.LOGGER.persisted(stepExecution, stepExecution.getStepExecutionId());
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        final String update = sqls.getProperty(UPDATE_STEP_EXECUTION);
        final Connection connection = getConnection();
        final StepExecutionImpl stepExecutionImpl = (StepExecutionImpl) stepExecution;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(update);
            preparedStatement.setTimestamp(1, createTimestamp(stepExecution.getEndTime()));
            preparedStatement.setString(2, stepExecution.getBatchStatus().name());
            preparedStatement.setString(3, stepExecution.getExitStatus());
            preparedStatement.setString(4, TableColumns.formatException(stepExecutionImpl.getException()));
            preparedStatement.setBytes(5, BatchUtil.objectToBytes(stepExecution.getPersistentUserData()));
            preparedStatement.setLong(6, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.READ_COUNT));
            preparedStatement.setLong(7, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.WRITE_COUNT));
            preparedStatement.setLong(8, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.COMMIT_COUNT));
            preparedStatement.setLong(9, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.ROLLBACK_COUNT));
            preparedStatement.setLong(10, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.READ_SKIP_COUNT));
            preparedStatement.setLong(11, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.PROCESS_SKIP_COUNT));
            preparedStatement.setLong(12, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.FILTER_COUNT));
            preparedStatement.setLong(13, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.WRITE_SKIP_COUNT));
            preparedStatement.setBytes(14, BatchUtil.objectToBytes(stepExecutionImpl.getReaderCheckpointInfo()));
            preparedStatement.setBytes(15, BatchUtil.objectToBytes(stepExecutionImpl.getWriterCheckpointInfo()));

            preparedStatement.setLong(16, stepExecution.getStepExecutionId());

            preparedStatement.executeUpdate();
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, update);
        } finally {
            close(connection, preparedStatement, null, null);
        }
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        //super.savePersistentData() serialize persistent data and checkpoint info to avoid further modification
        super.savePersistentData(jobExecution, stepOrPartitionExecution);
        if (stepOrPartitionExecution instanceof StepExecutionImpl) {
            //stepExecution is for the main step, and should map to the STEP_EXECUTIOIN table
            updateStepExecution(stepOrPartitionExecution);
        } else {
            //stepExecutionId is for a partition execution, and should map to the PARTITION_EXECUTION table
            final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) stepOrPartitionExecution;
            final String update = sqls.getProperty(UPDATE_PARTITION_EXECUTION);
            final Connection connection = getConnection();
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(update);
                preparedStatement.setString(1, partitionExecution.getBatchStatus().name());
                preparedStatement.setString(2, partitionExecution.getExitStatus());
                preparedStatement.setString(3, TableColumns.formatException(partitionExecution.getException()));
                preparedStatement.setBytes(4, BatchUtil.objectToBytes(partitionExecution.getPersistentUserData()));
                preparedStatement.setBytes(5, BatchUtil.objectToBytes(partitionExecution.getReaderCheckpointInfo()));
                preparedStatement.setBytes(6, BatchUtil.objectToBytes(partitionExecution.getWriterCheckpointInfo()));
                preparedStatement.setInt(7, partitionExecution.getPartitionId());
                preparedStatement.setLong(8, partitionExecution.getStepExecutionId());

                preparedStatement.executeUpdate();
            } catch (final Exception e) {
                throw BatchMessages.MESSAGES.failToRunQuery(e, update);
            } finally {
                close(connection, preparedStatement, null, null);
            }
        }
    }

    /*
    StepExecution selectStepExecution(final long stepExecutionId, final ClassLoader classLoader) {
        final String select = sqls.getProperty(SELECT_STEP_EXECUTION);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        final List<StepExecution> result = new ArrayList<StepExecution>();
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, stepExecutionId);
            rs = preparedStatement.executeQuery();
            createStepExecutionsFromResultSet(rs, result, false, classLoader);
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result.get(0);
    }
    */

    /**
     * Retrieves a list of StepExecution from database by JobExecution id.  This method does not check the cache, so it
     * should only be called after the cache has been searched without a match.
     *
     * @param jobExecutionId if null, retrieves all StepExecutions; otherwise, retrieves all StepExecutions belongs to the JobExecution id
     * @param classLoader    the current application class loader
     * @return a list of StepExecutions
     */
    @Override
    List<StepExecution> selectStepExecutions(final Long jobExecutionId, final ClassLoader classLoader) {
        final String select = (jobExecutionId == null) ? sqls.getProperty(SELECT_ALL_STEP_EXECUTIONS) :
                sqls.getProperty(SELECT_STEP_EXECUTIONS_BY_JOB_EXECUTION_ID);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        final List<StepExecution> result = new ArrayList<StepExecution>();
        try {
            preparedStatement = connection.prepareStatement(select);
            if (jobExecutionId != null) {
                preparedStatement.setLong(1, jobExecutionId);
            }
            rs = preparedStatement.executeQuery();
            createStepExecutionsFromResultSet(rs, result, false, classLoader);
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution, final PartitionExecutionImpl partitionExecution) {
        super.addPartitionExecution(enclosingStepExecution, partitionExecution);
        final String insert = sqls.getProperty(INSERT_PARTITION_EXECUTION);
        final Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insert);
            preparedStatement.setInt(1, partitionExecution.getPartitionId());
            preparedStatement.setLong(2, partitionExecution.getStepExecutionId());
            preparedStatement.setString(3, partitionExecution.getBatchStatus().name());
            preparedStatement.executeUpdate();
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, insert);
        } finally {
            close(connection, preparedStatement, null, null);
        }
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart,
                                                                 final ClassLoader classLoader) {
        final StepExecutionImpl result = super.findOriginalStepExecutionForRestart(stepName, jobExecutionToRestart, classLoader);
        if (result != null) {
            return result;
        }
        final String select = sqls.getProperty(FIND_ORIGINAL_STEP_EXECUTION);
        final Connection connection = getConnection();
        ResultSet rs = null;
        final List<StepExecution> results = new ArrayList<StepExecution>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, jobExecutionToRestart.getJobInstance().getInstanceId());
            preparedStatement.setString(2, stepName);
            rs = preparedStatement.executeQuery();
            createStepExecutionsFromResultSet(rs, results, true, classLoader);
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return results.size() > 0 ? (StepExecutionImpl) results.get(0) : null;
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
                                                               final StepExecutionImpl stepExecution,
                                                               final boolean notCompletedOnly,
                                                               final ClassLoader classLoader) {
        List<PartitionExecutionImpl> result = super.getPartitionExecutions(stepExecutionId, stepExecution, notCompletedOnly, classLoader);
        if (result != null && !result.isEmpty()) {
            return result;
        }
        final String select = sqls.getProperty(SELECT_PARTITION_EXECUTIONS_BY_STEP_EXECUTION_ID);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        result = new ArrayList<PartitionExecutionImpl>();
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setLong(1, stepExecutionId);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                final String batchStatusValue = rs.getString(TableColumns.BATCHSTATUS);
                if (!notCompletedOnly ||
                        !BatchStatus.COMPLETED.name().equals(batchStatusValue)) {
                    result.add(new PartitionExecutionImpl(
                            rs.getInt(TableColumns.PARTITIONEXECUTIONID),
                            rs.getLong(TableColumns.STEPEXECUTIONID),
                            stepExecution.getStepName(),
                            BatchStatus.valueOf(batchStatusValue),
                            rs.getString(TableColumns.EXITSTATUS),
                            BatchUtil.bytesToSerializableObject(rs.getBytes(TableColumns.PERSISTENTUSERDATA), classLoader),
                            BatchUtil.bytesToSerializableObject(rs.getBytes(TableColumns.READERCHECKPOINTINFO), classLoader),
                            BatchUtil.bytesToSerializableObject(rs.getBytes(TableColumns.WRITERCHECKPOINTINFO), classLoader)
                    ));
                }
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return result;
    }

    private void createStepExecutionsFromResultSet(final ResultSet rs,
                                                   final List<StepExecution> result,
                                                   final boolean top1,
                                                   final ClassLoader classLoader)
            throws SQLException, ClassNotFoundException, IOException {
        while (rs.next()) {
            final StepExecutionImpl e = new StepExecutionImpl(
                    rs.getLong(TableColumns.STEPEXECUTIONID),
                    rs.getString(TableColumns.STEPNAME),
                    rs.getTimestamp(TableColumns.STARTTIME),
                    rs.getTimestamp(TableColumns.ENDTIME),
                    rs.getString(TableColumns.BATCHSTATUS),
                    rs.getString(TableColumns.EXITSTATUS),
                    BatchUtil.bytesToSerializableObject(rs.getBytes(TableColumns.PERSISTENTUSERDATA), classLoader),
                    rs.getInt(TableColumns.READCOUNT),
                    rs.getInt(TableColumns.WRITECOUNT),
                    rs.getInt(TableColumns.COMMITCOUNT),
                    rs.getInt(TableColumns.ROLLBACKCOUNT),
                    rs.getInt(TableColumns.READSKIPCOUNT),
                    rs.getInt(TableColumns.PROCESSSKIPCOUNT),
                    rs.getInt(TableColumns.FILTERCOUNT),
                    rs.getInt(TableColumns.WRITESKIPCOUNT),
                    BatchUtil.bytesToSerializableObject(rs.getBytes(TableColumns.READERCHECKPOINTINFO), classLoader),
                    BatchUtil.bytesToSerializableObject(rs.getBytes(TableColumns.WRITERCHECKPOINTINFO), classLoader)
            );
            result.add(e);
            if (top1) {
                return;
            }
        }
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        final String select = sqls.getProperty(COUNT_STEP_EXECUTIONS_BY_JOB_INSTANCE_ID);
        final Connection connection = getConnection();
        ResultSet rs = null;
        PreparedStatement preparedStatement = null;
        int count = 0;
        try {
            preparedStatement = connection.prepareStatement(select);
            preparedStatement.setString(1, stepName);
            preparedStatement.setLong(2, jobInstanceId);
            rs = preparedStatement.executeQuery();

            while (rs.next()) {
                count = rs.getInt(1);
                break;
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, select);
        } finally {
            close(connection, preparedStatement, null, rs);
        }
        return count;
    }

    /**
     * Executes a series of sql statements.
     *
     * @param statements             sql statements as string separated with ; character; not null unless {@code statementsResourcePath} is present.
     * @param statementsResourcePath loadable resource path to obtain sql statements. Ignored when {@code statements} is present.
     * @throws SQLException
     */
    public void executeStatements(final String statements, final String statementsResourcePath) throws SQLException {
        final String delim = ";";
        final Connection connection = getConnection();
        final List<String> statementList;
        if (statements == null) {
            InputStream statementsResource = null;
            try {
                statementsResource = getClassLoader().getResourceAsStream(statementsResourcePath);
                if (statementsResource != null) {
                    statementList = new ArrayList<String>();
                } else {
                    throw BatchMessages.MESSAGES.failToLoadSqlProperties(null, statementsResourcePath);
                }

                final java.util.Scanner scanner = new java.util.Scanner(statementsResource).useDelimiter(delim);
                while (scanner.hasNext()) {
                    statementList.add(scanner.next());
                }
                scanner.close();
            } finally {
                if (statementsResource != null) {
                    try {
                        statementsResource.close();
                    } catch (final IOException e) {
                        //ignore
                    }
                }
            }
        } else {
            statementList = Arrays.asList(statements.split(delim));
        }

        Statement st = null;
        try {
            st = connection.createStatement();
            if (statementList.size() <= 1) {
                st.executeUpdate(statementList.get(0));
            } else {
                for (final String s : statementList) {
                    final String sqlEntry = s.trim();
                    if (!sqlEntry.isEmpty()) {
                        st.addBatch(sqlEntry);
                    }
                }
                st.executeBatch();
            }
        } finally {
            close(connection, st, null, null);
        }
    }

    private Connection getConnection() {
        if (dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (final SQLException e) {
                throw BatchMessages.MESSAGES.failToObtainConnection(e, dataSource);
            }
        } else {
            try {
                return DriverManager.getConnection(dbUrl, dbProperties);
            } catch (final Exception e) {
                throw BatchMessages.MESSAGES.failToObtainConnection(e, dbUrl, dbProperties);
            }
        }
    }

    private void close(final Connection conn, final Statement stmt1, final Statement stmt2, final ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (final SQLException e) {
                BatchLogger.LOGGER.failToClose(e, ResultSet.class, rs);
            }
        }

        if (stmt1 != null) {
            try {
                stmt1.close();
            } catch (final SQLException e) {
                BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, stmt1);
            }
        }

        if (stmt2 != null) {
            try {
                stmt2.close();
            } catch (final SQLException e) {
                BatchLogger.LOGGER.failToClose(e, PreparedStatement.class, stmt2);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (final SQLException e) {
                BatchLogger.LOGGER.failToClose(e, Connection.class, conn);
            }
        }
    }

    /**
     * If {@value #DDL_FILE_NAME_KEY} property is explicitly set, use it;
     * else determine the appropriate ddl file location based on {@code databaseProductName};
     * if no match, then return the default value {@value #DEFAULT_DDL_FILE}.
     *
     * @param databaseProductName a valid database product name, or "".  Must not be null
     * @return location of ddl file resource, e.g., sql/jberet.ddl
     */
    private String getDDLLocation(final String databaseProductName) {
        String ddlFile = userDefinedDdlFile;
        if (ddlFile != null) {
            ddlFile = ddlFile.trim();
            if (!ddlFile.isEmpty()) {
                BatchLogger.LOGGER.ddlFileAndDatabaseProductName(ddlFile, databaseProductName);
                return ddlFile;
            }
        }
        if (databaseProductName.contains("MySQL")) {
            ddlFile = "sql/jberet-mysql.ddl";
        } else if (databaseProductName.startsWith("Oracle")) {
            ddlFile = "sql/jberet-oracle.ddl";
        } else if (databaseProductName.contains("PostgreSQL") || databaseProductName.contains("EnterpriseDB")) {
            ddlFile = "sql/jberet-postgresql.ddl";
        } else if (databaseProductName.startsWith("Microsoft SQL Server")) {
            ddlFile = "sql/jberet-mssqlserver.ddl";
        } else if (databaseProductName.contains("DB2")) {
            ddlFile = "sql/jberet-db2.ddl";
        } else if (databaseProductName.contains("Adaptive Server Enterprise") || databaseProductName.contains("Sybase")) {
            ddlFile = "sql/jberet-sybase.ddl";
        } else if (databaseProductName.contains("Derby")) {
            ddlFile = "sql/jberet-derby.ddl";
        } else if (databaseProductName.startsWith("Firebird")) {
            ddlFile = "sql/jberet-firebird.ddl";
        } else {
            // H2, HSQLDB
            ddlFile = DEFAULT_DDL_FILE;
        }
        BatchLogger.LOGGER.ddlFileAndDatabaseProductName(ddlFile, databaseProductName);
        return ddlFile;
    }

    private static ClassLoader getClassLoader() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return JdbcRepository.class.getClassLoader();
                }
            });
        }
        return JdbcRepository.class.getClassLoader();
    }

    private static Timestamp createTimestamp(final Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }
}
