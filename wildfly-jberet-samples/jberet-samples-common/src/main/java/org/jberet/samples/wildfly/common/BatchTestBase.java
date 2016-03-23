/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.common;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Base class to be extended by concrete batch test cases.
 */
public abstract class BatchTestBase {
    protected static final String BASE_URL = "http://localhost:8080/";

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.printf("Starting test: %s%n", description.getMethodName());
        }
    };

    protected abstract BatchClient getBatchClient();

    /**
     * Starts a job, waits for it to finish, verifies the job execution batch status, and returns the
     * job execution id.
     *
     * @param jobName             the job name
     * @param queryParams         any query parameters to be passed as part of the REST request query parameters
     * @param waitMillis          number of milliseconds to wait for the job execution to finish
     * @param expectedBatchStatus expected job execution batch status
     * @return the job execution entity
     * @throws Exception if errors occurs
     */
    protected JobExecutionEntity startJobCheckStatus(final String jobName,
                                                     final Properties queryParams,
                                                     final long waitMillis,
                                                     final BatchStatus expectedBatchStatus) throws Exception {
        final JobExecutionEntity jobExecution = getBatchClient().startJob(jobName, queryParams);
        return getCheckJobExecution(jobExecution.getExecutionId(), waitMillis, expectedBatchStatus);
    }

    /**
     * Finds the latest job execution for a job id, restarts it, waits for it to finish, and verifies the
     * job execution batch status.
     *
     * @param jobName job Id
     * @param queryParams any query parameters to be passed as part of the REST request query parameters
     * @param waitMillis number of milliseconds to wait for the job execution to finish
     * @param expectedBatchStatus expected job execution batch status
     * @throws Exception if errors occurs
     *
     */
    protected void restartJobCheckStatus(final String jobName,
                                         final Properties queryParams,
                                         final long waitMillis,
                                         final BatchStatus expectedBatchStatus) throws Exception {
        final JobExecutionEntity jobExecution = getBatchClient().restartJobExecution(jobName, queryParams);
        getCheckJobExecution(jobExecution.getExecutionId(), waitMillis, expectedBatchStatus);
    }

    /**
     * Waits for milliseconds specified in {@code waitMillis}, retrieves the job execution, and checks its
     * batch status against {@code expectedBatchStatus}.
     *
     * @param jobExecutionId      job execution id
     * @param waitMillis          number of milliseconds to wait for the job execution to finish
     * @param expectedBatchStatus expected job execution batch status
     * @return the retrieves job execution
     * @throws Exception if errors occurs
     */
    private JobExecutionEntity getCheckJobExecution(final long jobExecutionId,
                                                    final long waitMillis,
                                                    final BatchStatus expectedBatchStatus) throws Exception {
        Thread.sleep(waitMillis);
        final JobExecutionEntity jobExecution2 = getBatchClient().getJobExecution(jobExecutionId);
        Assert.assertEquals(expectedBatchStatus, jobExecution2.getBatchStatus());
        return jobExecution2;
    }
}
