/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.purgeInMemoryRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobInstanceException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;

import org.jberet.repository.JobExecutionSelector;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.spi.PropertyKey;
import org.jberet.testapps.common.AbstractIT;

public abstract class PurgeRepositoryTestBase extends AbstractIT {
    protected static final long purgeSleepMillis = 2000;
    protected static final String prepurgeJobName = "prepurge";
    protected static final String prepurge2JobName = "prepurge2";
    protected static final String prepurgeAndPrepurge2JobNames = "prepurge, prepurge2";
    protected static final String chunkPartitionJobXml = "org.jberet.test.chunkPartition";

    public long prepurge(final String... jobName) throws Exception {
        final String prepurgeJobName = (jobName.length == 0) ? PurgeRepositoryTestBase.prepurgeJobName : jobName[0];
        startJob(prepurgeJobName);
        awaitTermination();
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("%s job execution id: %s, status: %s%n", prepurgeJobName, jobExecutionId, jobExecution.getBatchStatus());
        return jobExecutionId;
    }

    public void startAndVerifyPurgeJob(final String purgeJobXml) throws Exception {
        startJob(purgeJobXml);
        awaitTermination();

        //the current job will not be purged, and should complete
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertNotNull(jobOperator.getJobExecution(jobExecutionId));
    }

    protected void noSuchJobException() throws Exception {
        final String[] noSuchJobNames = {"no-such-job-name", null, ""};
        for (final String noSuchJobName : noSuchJobNames) {
            try {
                final int result = jobOperator.getJobInstanceCount(noSuchJobName);
                fail("Expecting NoSuchJobException, but got " + result);
            } catch (final NoSuchJobException e) {
                System.out.printf("Got expected %s%n", e);
            }

            try {
                fail("Expecting NoSuchJobException, but got " + jobOperator.getJobInstances(noSuchJobName, 0, 1));
            } catch (final NoSuchJobException e) {
                System.out.printf("Got expected %s%n", e);
            }

            try {
                fail("Expecting NoSuchJobException, but got " + jobOperator.getRunningExecutions(noSuchJobName));
            } catch (final NoSuchJobException e) {
                System.out.printf("Got expected %s%n", e);
            }
        }
    }

    protected void noSuchJobInstanceException() throws Exception {
        JobInstanceImpl invalidJobInstance = new JobInstanceImpl(null, null, "xxxxxxxxxxxxxxx");
        try {
            final List<JobExecution> result = jobOperator.getJobExecutions(invalidJobInstance);
            if (result.isEmpty()) {
                System.out.printf("Got expected result: %s%n", result);
            } else {
                fail("Expecting NoSuchJobInstanceException, but got " + result);
            }
        } catch (final NoSuchJobInstanceException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    /**
     * Starts and wait for the job to finish, and then call getRunningExecutions(jobName), which should return
     * empty List<Long>, since no job with jobName is running.
     *
     * @throws Exception
     */
    protected void getRunningExecutions() throws Exception {
        prepurge();
        final List<Long> runningExecutions = jobOperator.getRunningExecutions(prepurgeJobName);
        assertEquals(0, runningExecutions.size());
    }

    /**
     * Starts a job without waiting for it to finish, and then call getRunningExecutions(jobName), which should return
     * 1-element List<Long>. The job execution launches javascript engine (the batchlet is inline javascript) and so
     * should still be running when the test calls getRunningExecutions.
     *
     * @throws Exception
     */
    protected void getRunningExecutions2() throws Exception {
        startJob(prepurgeJobName);
        final List<Long> runningExecutions = jobOperator.getRunningExecutions(prepurgeJobName);
        assertEquals(1, runningExecutions.size());
        awaitTermination();
    }

    protected void getJobExecutionsByJob() throws Exception {
        final int loopCount = 3;
        for (int i = 0; i < loopCount; i++) {
            startJob(prepurgeJobName);
            startJob(prepurge2JobName);
        }

        // get job executionIds for job name prepurge
        List<Long> executionIds = jobOperator.getJobExecutionsByJob(prepurgeJobName);
        assertEquals(loopCount, executionIds.size());
        assertTrue(executionIds.get(0) > executionIds.get(1) && executionIds.get(1) > executionIds.get(2));
        assertEquals(prepurgeJobName, jobOperator.getJobExecution(executionIds.get(0)).getJobName());
        assertEquals(prepurgeJobName, jobOperator.getJobExecution(executionIds.get(1)).getJobName());
        assertEquals(prepurgeJobName, jobOperator.getJobExecution(executionIds.get(2)).getJobName());

        // get job executionIds for job name prepurge2
        executionIds = jobOperator.getJobExecutionsByJob(prepurge2JobName);
        assertEquals(loopCount, executionIds.size());
        assertTrue(executionIds.get(0) > executionIds.get(1) && executionIds.get(1) > executionIds.get(2));
        assertEquals(prepurge2JobName, jobOperator.getJobExecution(executionIds.get(0)).getJobName());
        assertEquals(prepurge2JobName, jobOperator.getJobExecution(executionIds.get(1)).getJobName());
        assertEquals(prepurge2JobName, jobOperator.getJobExecution(executionIds.get(2)).getJobName());

        // get all job executionIds
        List<JobExecution> allExecutions = jobOperator.getJobExecutions(null);
        assertTrue(allExecutions.size() >= loopCount * 2);

        // get job executions by invalid job name should return empty list
        executionIds = jobOperator.getJobExecutionsByJob("invalid job name");
        assertEquals(0, executionIds.size());
    }

    protected void memoryTest() throws Exception {
        final int times = Integer.getInteger("times", 5000);
        for (int i = 0; i < times; i++) {
            System.out.printf("================ %s ================ %n", i);

            params = new Properties();

            //add more job parameters to consume memory
            final String val = System.getProperty("user.dir");
            for (int n = 0; n < 20; n++) {
                params.setProperty(String.valueOf(n), val);
            }

            params.setProperty("thread.count", "10");
            params.setProperty("skip.thread.check", "true");
            params.setProperty("writer.sleep.time", "0");
            startJobAndWait(chunkPartitionJobXml);
            assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        }
    }

    protected void ctrlC() throws Exception {
        params.setProperty("thread.count", "2");
        params.setProperty("skip.thread.check", "true");
        params.setProperty("writer.sleep.time", "3000");
        startJobAndWait(chunkPartitionJobXml);
    }

    protected void invalidRestartMode() throws Exception {
        final Properties restartParams = new Properties();
        restartParams.setProperty(PropertyKey.RESTART_MODE, "auto");
        restartKilled(restartParams);
    }

    protected void restartKilledStrict() throws Exception {
        final Properties restartParams = new Properties();
        restartParams.setProperty(PropertyKey.RESTART_MODE, PropertyKey.RESTART_MODE_STRICT);
        restartKilled(restartParams);
    }

    protected void restartKilled() throws Exception {
        restartKilled(null);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    protected void restartKilledStopAbandon() throws Exception {
        final long originalJobExecutionId = getOriginalJobExecutionId(chunkPartitionJobXml);
        params.setProperty("writer.sleep.time", "0");

        final long restartExecutionId = jobOperator.restart(originalJobExecutionId, null);
        final JobExecutionImpl restartExecution = (JobExecutionImpl) jobOperator.getJobExecution(restartExecutionId);
        jobOperator.stop(restartExecutionId);
        restartExecution.awaitTermination(5, TimeUnit.MINUTES);
        jobOperator.abandon(restartExecutionId);
        jobOperator.abandon(originalJobExecutionId);
        assertEquals(BatchStatus.ABANDONED, jobOperator.getJobExecution(originalJobExecutionId).getBatchStatus());
        assertEquals(BatchStatus.ABANDONED, restartExecution.getBatchStatus());
    }

    protected void restartKilledForce() throws Exception {
        final Properties restartParams = new Properties();
        restartParams.setProperty(PropertyKey.RESTART_MODE, PropertyKey.RESTART_MODE_FORCE);
        restartKilled(restartParams);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    protected void restartKilledDetect() throws Exception {
        final Properties restartParams = new Properties();
        restartParams.setProperty(PropertyKey.RESTART_MODE, PropertyKey.RESTART_MODE_DETECT);
        restartKilled(restartParams);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    private void restartKilled(final Properties restartParams) throws InterruptedException {
        final long originalJobExecutionId = getOriginalJobExecutionId(chunkPartitionJobXml);
        params.setProperty("writer.sleep.time", "0");
        if (restartParams != null) {
            params.putAll(restartParams);
        }
        restartAndWait(originalJobExecutionId);
    }

    public static final class JobExecutionSelector1 implements JobExecutionSelector {
        private JobContext jobContext;
        private StepContext stepContext;

        @Override
        public boolean select(final JobExecution jobExecution,
                              final Collection<Long> allJobExecutionIds) {
            //select completed job executions and whose job name starts with "pre"
            if (jobExecution.getBatchStatus() == BatchStatus.COMPLETED && jobExecution.getJobName().startsWith("pre")) {
                System.out.printf("In select method of %s, return true.%n", this);
                return true;
            }
            System.out.printf("In select method of %s, return false.%n", this);
            return false;
        }

        @Override
        public JobContext getJobContext() {
            return jobContext;
        }

        @Override
        public void setJobContext(final JobContext jobContext) {
            this.jobContext = jobContext;
        }

        @Override
        public StepContext getStepContext() {
            return stepContext;
        }

        @Override
        public void setStepContext(final StepContext stepContext) {
            this.stepContext = stepContext;
        }
    }
}
