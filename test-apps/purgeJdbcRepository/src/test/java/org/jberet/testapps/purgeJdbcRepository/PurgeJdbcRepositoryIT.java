/*
 * Copyright (c) 2015-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.purgeJdbcRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import org.jberet.repository.JdbcRepository;
import org.jberet.se.BatchSEEnvironment;
import org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PurgeJdbcRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeJdbcRepositoryJobName = "purgeJdbcRepository";

    /////////////////////////////////////////////////////
    @Test
    @Ignore("run it manually, Ctrl-C before it completes")
    public void ctrlC_1() throws Exception {
        super.ctrlC();
    }

    @Test(expected = JobRestartException.class)
    @Ignore("run after ctrlC_1 test has been killed with invalid restart mode, should fail")
    public void invalidRestartMode() throws Exception {
        super.invalidRestartMode();
    }

    @Test(expected = JobRestartException.class)
    @Ignore("run after ctrlC_1 test has been killed, should fail")
    public void restartKilledStrict() throws Exception {
        super.restartKilledStrict();
    }

    /////////////////////////////////////////////////////
    @Test
    @Ignore("run it manually, Ctrl-C before it completes")
    public void ctrlC_2() throws Exception {
        super.ctrlC();
    }

    @Test
    @Ignore("run after ctrlC_2 test has been killed")
    public void restartKilled() throws Exception {
        super.restartKilled();
    }

    /////////////////////////////////////////////////////
    @Test
    @Ignore("run it manually, Ctrl-C before it completes")
    public void ctrlC_3() throws Exception {
        super.ctrlC();
    }

    @Test
    @Ignore("run after ctrlC_3 test has been killed")
    public void restartKilledDetect() throws Exception {
        super.restartKilledDetect();
    }

    /////////////////////////////////////////////////////
    @Test
    @Ignore("run it manually, Ctrl-C before it completes")
    public void ctrlC_4() throws Exception {
        super.ctrlC();
    }

    @Test
    @Ignore("run after ctrlC_4 test has been killed")
    public void restartKilledForce() throws Exception {
        super.restartKilledForce();
    }

    /////////////////////////////////////////////////////
    @Test
    @Ignore("run it manually, Ctrl-C before it completes")
    public void ctrlC_5() throws Exception {
        super.ctrlC();
    }

    @Test
    @Ignore("run after ctrlC_5 test has been killed")
    public void restartKilledStopAbandon() throws Exception {
        super.restartKilledStopAbandon();
    }

    /////////////////////////////////////////////////////
    @Test
    @Ignore("run it manually")
    public void memoryTest() throws Exception {
        super.memoryTest();
    }

    /////////////////////////////////////////////////////
    @Test(expected = NoSuchJobExecutionException.class)
    public void restartNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.restart(-1, null);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void stopNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.stop(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void abandonNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.abandon(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void getParametersNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.getParameters(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void getJobInstanceNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.getJobInstance(-1);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void getStepExecutionsNoSuchJobExecutionException() throws NoSuchJobExecutionException {
        jobOperator.getStepExecutions(-1);
    }

    @Test
    public void getRunningExecutions() throws Exception {
        purgeJobExecutions();
        super.getRunningExecutions();
    }

    @Test
    public void getRunningExecutions2() throws Exception {
        purgeJobExecutions();
        super.getRunningExecutions2();
    }

    @Test
    public void getJobExecutionsByJob() throws Exception {
        purgeJobExecutions();
        super.getJobExecutionsByJob();
    }

    @Test
    public void getJobExecutionsByJobWithLimit() throws Exception {
        purgeJobExecutions();
        super.getJobExecutionsByJobWithLimit();
    }

    @Test
    public void withSql() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sql",
                "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
                        "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

                        "delete from JOB_EXECUTION where JOBINSTANCEID in " +
                        "(select DISTINCT JOBINSTANCEID from JOB_INSTANCE where JOBNAME like 'prepurge%');"
        );

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void deleteJobInstancesWithSql() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge1JobExecutionId).getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge2JobExecutionId).getBatchStatus());
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurgeJobName));
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurge2JobName));
        Assert.assertNotNull(jobOperator.getJobInstances(prepurgeJobName, 0, 1).get(0));
        Assert.assertNotNull(jobOperator.getJobInstances(prepurge2JobName, 0, 1).get(0));
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurgeJobName).size());
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurge2JobName).size());

        params.setProperty("sql",
                "delete from STEP_EXECUTION where JOBEXECUTIONID in " +
                        "(select JOBEXECUTIONID from JOB_EXECUTION, JOB_INSTANCE " +
                        "where JOB_EXECUTION.JOBINSTANCEID = JOB_INSTANCE.JOBINSTANCEID and JOB_INSTANCE.JOBNAME like 'prepurge%'); " +

                        "delete from JOB_EXECUTION where JOBINSTANCEID in " +
                        "(select DISTINCT JOBINSTANCEID from JOB_INSTANCE where JOBNAME like 'prepurge%'); " +

                        "delete from JOB_INSTANCE where JOBNAME like 'prepurge%' "
        );

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
        Assert.assertEquals(0, jobOperator.getJobInstanceCount(prepurgeJobName));
        Assert.assertEquals(0, jobOperator.getJobInstanceCount(prepurge2JobName));
        Assert.assertEquals(0, jobOperator.getJobInstances(prepurgeJobName, 0, 1).size());
        Assert.assertEquals(0, jobOperator.getJobInstances(prepurge2JobName, 0, 1).size());
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurgeJobName).size());
        Assert.assertEquals(0, jobOperator.getRunningExecutions(prepurge2JobName).size());
    }

    @Test
    @Override
    public void noSuchJobException() throws Exception {
        super.noSuchJobException();
    }

    @Test
    @Override
    public void noSuchJobInstanceException() throws Exception {
        super.noSuchJobInstanceException();
    }

    @Test
    public void withSqlFile() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sqlFile", "purgeJdbcRepository.sql");

        //prepurge2 job execution is purged from in-memory part, but still kept in database.
        //So next when calling getJobExecution(prepurge2JobExecutionId) should retrieve it from the database, and return
        //non-null.
        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(prepurge2JobExecutionId).getBatchStatus());
    }

    @Test
    public void withSqlDeleteJobInstancesCascade() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sql", "delete from JOB_INSTANCE where JOBNAME like 'prepurge%'");
        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        try {
            Assert.fail("Expecting NoSuchJobException, but got " + jobOperator.getJobInstanceCount(prepurgeJobName));
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobException, but got " + jobOperator.getJobInstanceCount(prepurge2JobName));
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge1JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge2JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void withSqlDeleteJobExecutionsCascade() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("sql",
                "delete from JOB_EXECUTION where JOBINSTANCEID in " +
                    "(select DISTINCT JOBINSTANCEID from JOB_INSTANCE where JOBNAME like 'prepurge%'); ");

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurgeJobName));
        Assert.assertNotEquals(0, jobOperator.getJobInstanceCount(prepurge2JobName));

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge1JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }

        try {
            Assert.fail("Expecting NoSuchJobExecutionException, but got" + jobOperator.getStepExecutions(prepurge2JobExecutionId));
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    /**
     * Verifies that a jdbc job repository can be created concurrently by multiple client without failure.
     * In Java SE environment, it's already synchronized in {@link org.jberet.se.JobRepositoryFactory}, but in
     * WildFly environment, especially in domain mode, multiple clients are in different JVM, and 2 clients may both
     * see the table does not exist, but only the 1st client will be able to create tables successfully, and the 2nd
     * one will failure because these tables have already been created by the 1st client.
     * <p/>
     * Note that this issue only affect those DBMS products that do not support "CREATE IF NOT EXIST" clause, such as
     * Derby, Oracle, DB2, Sybase, etc. For DBMS products that support "CREATE IF NOT EXIST" clause (e.g., H2), it is
     * not an issue.  That's also why this test uses embedded derby instead of H2.
     * <p/>
     * This test directly calls {@link JdbcRepository#create(Properties)} in order to bypass the synchronized path of
     * {@link org.jberet.se.JobRepositoryFactory}.
     * <p/>
     * This test does not start any batch job.
     *
     * @throws Exception
     * @see JdbcRepository#create(Properties)
     * @see <a href="https://issues.jboss.org/browse/WFLY-5134">WFLY-5134</a>
     * @see <a href="https://issues.jboss.org/browse/JBERET-185">JBERET-185</a>
     */
    @Test
    public void concurrentCreateJdbcJobRepository() throws Exception {
        final int count = 10;
        Connection conn = null;
        final String embeddedDerbyUrl = "jdbc:derby:target/derby;create=true";
        final Properties props = new Properties();
        props.setProperty(JdbcRepository.DB_URL_KEY, embeddedDerbyUrl);
        props.setProperty(BatchSEEnvironment.JOB_REPOSITORY_TYPE_KEY, BatchSEEnvironment.REPOSITORY_TYPE_JDBC);

        try {
            conn = DriverManager.getConnection(embeddedDerbyUrl);
            final Statement drop = conn.createStatement();
            dropTableIgnoreException(drop, "PARTITION_EXECUTION");
            dropTableIgnoreException(drop, "STEP_EXECUTION");
            dropTableIgnoreException(drop, "JOB_EXECUTION");
            dropTableIgnoreException(drop, "JOB_INSTANCE");
            System.out.printf("Dropped 4 tables%n");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (final SQLException sqle) {
                    System.err.printf("Exception while closing Connection:%n");
                    sqle.printStackTrace();
                }
            }
        }

        final ExecutorService executor = Executors.newCachedThreadPool();
        final List<Callable<Exception>> tasks = new ArrayList<Callable<Exception>>();

        for (int i = 0; i < count; i++) {
            tasks.add(new Callable<Exception>() {
                @Override
                public Exception call() {
                    try {
                        final JdbcRepository jdbcRepository = JdbcRepository.create(props);
                        return null;
                    } catch (final Exception e) {
                        return e;
                    }
                }
            });
        }

        final List<Future<Exception>> results = executor.invokeAll(tasks);
        System.out.printf("All exceptions while trying to create tables: %n");
        int failedCount = 0;
        for (final Future<Exception> e : results) {
            final Exception ex = e.get();
            if (ex != null) {
                failedCount++;
                System.out.printf("%n%s%n", ex);
                if (ex.getCause() != null) {
                    System.out.printf("%s%n", ex.getCause().toString());
                }
            }
        }
        executor.shutdown();
        if (failedCount > 0) {
            Assert.fail("Trying to create tables concurrently with " + count + " threads, " + failedCount + " failed.");
        }
    }

    public static void dropTableIgnoreException(final Statement dropStatement, final String tableName) {
        try {
            dropStatement.executeUpdate("DROP TABLE " + tableName);
        } catch (final SQLException e) {
            System.out.printf("Exception while dropping tables: %s%n", e.toString());
        }
    }

    private void purgeJobExecutions() throws Exception {
        // first purge existing job execution data from db to have a clean start
        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);
        startAndVerifyPurgeJob(purgeJdbcRepositoryJobName);
        final JdbcRepository jdbcRepository = (JdbcRepository) jobOperator.getJobRepository();
        jdbcRepository.executeStatements( "delete from PARTITION_EXECUTION; delete from STEP_EXECUTION; delete from JOB_EXECUTION; delete from JOB_INSTANCE", null);

    }
}
