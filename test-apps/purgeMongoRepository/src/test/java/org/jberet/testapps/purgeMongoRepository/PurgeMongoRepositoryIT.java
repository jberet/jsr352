/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.purgeMongoRepository;

import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobInstance;

import org.jberet.testapps.purgeInMemoryRepository.PurgeRepositoryTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PurgeMongoRepositoryIT extends PurgeRepositoryTestBase {
    static final String purgeMongoRepositoryJobName = "purgeMongoRepository";

    /////////////////////////////////////////////////////
    @Test
    @Disabled("run it manually, Ctrl-C before it completes")
    public void ctrlC_1() throws Exception {
        super.ctrlC();
    }

    @Test
    @Disabled("run after ctrlC_1 test has been killed with invalid restart mode, should fail")
    public void invalidRestartMode() throws Exception {
        assertThrows(JobRestartException.class, super::invalidRestartMode);
    }

    @Test
    @Disabled("run after ctrlC_1 test has been killed, should fail")
    public void restartKilledStrict() throws Exception {
        assertThrows(JobRestartException.class, super::restartKilledStrict);
    }

    /////////////////////////////////////////////////////
    @Test
    @Disabled("run it manually, Ctrl-C before it completes")
    public void ctrlC_2() throws Exception {
        super.ctrlC();
    }

    @Test
    @Disabled("run after ctrlC_2 test has been killed")
    public void restartKilled() throws Exception {
        super.restartKilled();
    }

    /////////////////////////////////////////////////////
    @Test
    @Disabled("run it manually, Ctrl-C before it completes")
    public void ctrlC_3() throws Exception {
        super.ctrlC();
    }

    @Test
    @Disabled("run after ctrlC_3 test has been killed")
    public void restartKilledDetect() throws Exception {
        super.restartKilledDetect();
    }

    /////////////////////////////////////////////////////
    @Test
    @Disabled("run it manually, Ctrl-C before it completes")
    public void ctrlC_4() throws Exception {
        super.ctrlC();
    }

    @Test
    @Disabled("run after ctrlC_4 test has been killed")
    public void restartKilledForce() throws Exception {
        super.restartKilledForce();
    }

    /////////////////////////////////////////////////////
    @Test
    @Disabled("run it manually, Ctrl-C before it completes")
    public void ctrlC_5() throws Exception {
        super.ctrlC();
    }

    @Test
    @Disabled("run after ctrlC_5 test has been killed")
    public void restartKilledStopAbandon() throws Exception {
        super.restartKilledStopAbandon();
    }

    /////////////////////////////////////////////////////
    @Test
    @Disabled("run it manually")
    public void memoryTest() throws Exception {
        super.memoryTest();
    }

    @Test
    public void restartNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.restart(-1, null));
    }

    @Test
    public void stopNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.stop(-1));
    }

    @Test
    public void abandonNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.abandon(-1));
    }

    @Test
    public void getParametersNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getParameters(-1));
    }

    @Test
    public void getJobInstanceNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getJobInstance(-1));
    }

    @Test
    public void getStepExecutionsNoSuchJobExecutionException() {
        assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getStepExecutions(-1));
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
    public void removeStepExecutionsAndJobExecutions() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}}); " +
                        "db. JOB_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}})");

        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);
    }

    @Test
    public void removeStepExecutionsAndJobExecutionsAndJobInstances() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);
        final long instanceId1 = jobOperator.getJobInstance(prepurge1JobExecutionId).getInstanceId();
        final long instanceId2 = jobOperator.getJobInstance(prepurge2JobExecutionId).getInstanceId();

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}}); " +
                "db. JOB_EXECUTION.remove({JOBEXECUTIONID: {$in: [" + prepurge1JobExecutionId + ", " + prepurge2JobExecutionId + "]}}); " +
                "db.  JOB_INSTANCE.remove({JOBINSTANCEID:  {$in: [" + instanceId1 + ", " + instanceId2 + "]}})"
        );

        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        try {
            final JobInstance ins = jobOperator.getJobInstance(prepurge1JobExecutionId);
            org.junit.Assertions.fail("Expecting NoSuchJobExecutionException, but got " + ins);
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
        try {
            final JobInstance ins = jobOperator.getJobInstance(prepurge2JobExecutionId);
            org.junit.Assertions.fail("Expecting NoSuchJobException, but got " + ins);
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }

    }

    @Test
    public void removeAll() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge(prepurge2JobName);
        final long instanceId1 = jobOperator.getJobInstance(prepurge1JobExecutionId).getInstanceId();
        final long instanceId2 = jobOperator.getJobInstance(prepurge2JobExecutionId).getInstanceId();

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove(); " +
                "db. JOB_EXECUTION.remove(); " +
                "db.  JOB_INSTANCE.remove()"
        );

        params.setProperty("purgeJobsByNames", prepurgeAndPrepurge2JobNames);

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);

        assertNoSuchJobExecution(prepurge1JobExecutionId);
        assertNoSuchJobExecution(prepurge2JobExecutionId);

        try {
            final int count = jobOperator.getJobInstanceCount(prepurgeJobName);
            org.junit.Assertions.fail("Expecting NoSuchJobException, but got " + count);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }
        try {
            final int count = jobOperator.getJobInstanceCount(prepurge2JobName);
            org.junit.Assertions.fail("Expecting NoSuchJobException, but got " + count);
        } catch (final NoSuchJobException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void invalidRemoveQueries() throws Exception {
        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove(1); " +
                "db. JOB_EXECUTION.remove(1); " +
                "db.  JOB_INSTANCE.remove(1)"
        );

        startJob(purgeMongoRepositoryJobName);
        awaitTermination();
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    private void purgeJobExecutions() throws Exception {
        params.setProperty("jobExecutionsByJobNames", prepurgeAndPrepurge2JobNames);

        params.setProperty("mongoRemoveQueries",
                "db.STEP_EXECUTION.remove({}); " +
                        "db. JOB_EXECUTION.remove({})");

        startAndVerifyPurgeJob(purgeMongoRepositoryJobName);
    }
}
