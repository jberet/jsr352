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

import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.JobInstanceEntity;
import org.jberet.rest.entity.StepExecutionEntity;
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
        final JobExecutionEntity jobExecution = startJob(jobName, queryParams);
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
        final JobExecutionEntity jobExecution = restartJobExecution(jobName, queryParams);
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
        final JobExecutionEntity jobExecution2 = getJobExecution(jobExecutionId);
        Assert.assertEquals(expectedBatchStatus, jobExecution2.getBatchStatus());
        return jobExecution2;
    }

    protected JobExecutionEntity startJob(final String jobXmlName, final Properties queryParams) throws Exception {
        final URI uri = getBatchClient().getJobUriBuilder("start").resolveTemplate("jobXmlName", jobXmlName).build();
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        return target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), JobExecutionEntity.class);
    }

    protected JobExecutionEntity restartJobExecution(final String jobXmlName, final Properties queryParams) throws Exception {
        final URI uri = getBatchClient().getJobUriBuilder("restart").resolveTemplate("jobXmlName", jobXmlName).build();
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        return target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), JobExecutionEntity.class);
    }

    protected WebTarget getTarget(final URI uri, final Properties props) {
        WebTarget result = getBatchClient().target(uri);

        if (props == null) {
            return result;
        } else {
            for (final String k : props.stringPropertyNames()) {
                result = result.queryParam(k, props.getProperty(k));
            }
        }
        return result;
    }

    protected JobExecutionEntity getJobExecution(final long jobExecutionId) {
        final URI uri = getBatchClient().getJobExecutionUriBuilder(null).path(String.valueOf(jobExecutionId)).build();
        System.out.printf("uri: %s%n", uri);
        final WebTarget target = getBatchClient().target(uri);
        return target.request().get(JobExecutionEntity.class);
    }

    protected JobInstanceEntity[] getJobInstances(final String jobName, final int start, final int count)
            throws Exception {
        final URI uri = getBatchClient().getJobInstanceUriBuilder(null).build();
        final WebTarget target = getBatchClient().target(uri)
                .queryParam("jobName", jobName)
                .queryParam("start", start)
                .queryParam("count", count);
        System.out.printf("uri: %s%n", uri);
        final JobInstanceEntity[] data = target.request().get(JobInstanceEntity[].class);

        System.out.printf("Got JobInstanceEntity[]: %s%n", Arrays.toString(data));
        return data;
    }

    protected StepExecutionEntity[] getStepExecutions(final long jobExecutionId) {
        final URI uri = getBatchClient().getJobExecutionUriBuilder("getStepExecutions")
                .resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = getTarget(uri, null);
        System.out.printf("uri: %s%n", uri);
        return target.request().get(StepExecutionEntity[].class);
    }
}
