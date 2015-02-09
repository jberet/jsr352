/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.purgeInMemoryRepository;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class PurgeInMemoryRepositoryIT extends AbstractIT {
    private static final long purgeSleepMillis = 2000;
    static final String prepurgeXml = "prepurge.xml";
    static final String purgeInMemoryRepositoryXml = "purgeInMemoryRepository.xml";

    @Test
    public void purgeAllJobs() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        params.setProperty("purgeAllJobs", "true");
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        //awaitTermination();
        //Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge2JobExecutionId));

        final long purgeJobExecutionId = prepurge2JobExecutionId + 1;
        Assert.assertEquals(null, jobOperator.getJobExecution(purgeJobExecutionId));
    }

    @Test
    public void jobExecutionIds() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //non-existent job execution id 999 will be ignored
        params.setProperty("jobExecutionIds", String.format("%s,%s,%s", prepurge1JobExecutionId, prepurge2JobExecutionId, 999));
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge2JobExecutionId));

        final long purgeJobExecutionId = prepurge2JobExecutionId + 1;
        Assert.assertNotNull(jobOperator.getJobExecution(purgeJobExecutionId));
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }


    @Test
    public void numberOfRecentJobExecutionsToKeep() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //2 job executions to keep:
        //the purge job itself
        //prepurge2
        params.setProperty("numberOfRecentJobExecutionsToKeep", "2");
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNotNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    @Test
    public void jobExecutionIdFrom() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id >= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        Assert.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
        final long purgeJobExecutionId = prepurge2JobExecutionId + 1;
        Assert.assertNull(jobOperator.getJobExecution(purgeJobExecutionId));
    }

    @Test
    public void jobExecutionIdTo() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id <= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdTo", String.valueOf(prepurge2JobExecutionId));
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        Assert.assertNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertNotNull(jobOperator.getJobExecution(jobExecutionId));
    }

    @Test
    public void jobExecutionIdFromTo() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();
        final long prepurge3JobExecutionId = prepurge();

        //purge job executions whose id between prepurge2JobExecutionId and prepurge3JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        params.setProperty("jobExecutionIdTo", String.valueOf(prepurge3JobExecutionId));
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        Assert.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge3JobExecutionId));
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertNotNull(jobOperator.getJobExecution(jobExecutionId));
    }

    @Test
    public void withinPastMinutes() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions which ended within past 5 minutes
        params.setProperty("withinPastMinutes", "5");
        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        Assert.assertNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));

        final long purgeJobExecutionId = prepurge2JobExecutionId + 1;
        Assert.assertEquals(null, jobOperator.getJobExecution(purgeJobExecutionId));
    }


    public long prepurge() throws Exception {
        startJob(prepurgeXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("%s job execution id: %s, status: %s%n", prepurgeXml, jobExecutionId, jobExecution.getBatchStatus());
        return jobExecutionId;
    }
}
