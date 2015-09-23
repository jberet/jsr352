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

package org.jberet.testapps.restapi;

import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jberet.rest.model.JobExecutionData;
import org.jberet.rest.model.JobInstanceData;
import org.jberet.rest.model.StepExecutionData;
import org.jberet.rest.resource.JobExecutionResource;
import org.jberet.rest.resource.JobInstanceResource;
import org.jberet.rest.resource.JobResource;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Ignore("Need to have a running WildFly server and deploy the test app before running tests")
public class RestAPIIT {
    private static final String jobName1 = "restJob1";
    private static final String jobName2 = "restJob2";
    private static final String jobWithParams = "restJobWithParams";
    private static final String jobNameBad = "xxxxxxx";

    // context-path: use war file base name as the default context root
    // rest api mapping url: configured in web.xml servlet-mapping
    private static final String restUrl = "http://localhost:8080/restAPI/api";
    private Client client = ClientBuilder.newClient();

    @Test
    public void start() throws Exception {
        final JobExecutionData data = startJob(jobName1, null);
        System.out.printf("Response entity: %s%n", data);
        Assert.assertNotNull(data.getCreateTime());
    }

    @Test
    public void startWithJobParams() throws Exception {
        final URI uri = getJobUriBuilder("start").resolveTemplate("jobXmlName", jobName2).build();
        final WebTarget target = client.target(uri)
                .queryParam("jobParam1", "jobParam1 value")
                .queryParam("jobParam2", "jobParam2 value");
        System.out.printf("uri: %s%n", uri);
        final Response response = target.request().post(emptyJsonEntity());

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void startWithBadJobXmlName() throws Exception {
        final URI uri = getJobUriBuilder("start").resolveTemplate("jobXmlName", jobNameBad).build();
        final WebTarget target = client.target(uri);
        System.out.printf("uri: %s%n", uri);
        final Response response = target.request().post(emptyJsonEntity());

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void getJobNames() throws Exception {
        final URI uri = getJobUriBuilder(null).build();
        final WebTarget target = client.target(uri);
        System.out.printf("uri: %s%n", uri);
        final Response response = target.request().get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getJobInstances() throws Exception {
        final JobExecutionData jobExecutionData = startJob(jobName1, null);// to have at least 1 job instance
        final URI uri = getJobInstanceUriBuilder(null).build();
        final WebTarget target = client.target(uri)
                .queryParam("jobName", jobExecutionData.getJobName())
                .queryParam("start", 0)
                .queryParam("count", 99999999);
        System.out.printf("uri: %s%n", uri);
        final JobInstanceData[] data = target.request().get(JobInstanceData[].class);

        System.out.printf("Got JobInstanceData[]: %s%n", Arrays.toString(data));
        assertEquals(jobName1, data[0].getJobName());
    }

    @Test
    public void getJobInstanceCount() throws Exception {
        startJob(jobName1, null);  // to have at least 1 job instance
        final URI uri = getJobInstanceUriBuilder("getJobInstanceCount").build();
        final WebTarget target = client.target(uri).queryParam("jobName", jobName1);
        System.out.printf("uri: %s%n", uri);
        final Integer count = target.request().get(int.class);

        assertEquals(true, count > 0);
    }

    @Test
    public void getJobInstanceCountBadJobName() throws Exception {
        final URI uri = getJobInstanceUriBuilder("getJobInstanceCount").build();
        final WebTarget target = client.target(uri).queryParam("jobName", jobNameBad);
        System.out.printf("uri: %s%n", uri);
        final Response response = target.request().get();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getJobInstance() throws Exception {
        final JobExecutionData jobExecutionData = startJob(jobName1, null);// to have at least 1 job instance
        final URI uri = getJobInstanceUriBuilder(null).build();
        final WebTarget target = client.target(uri).queryParam("jobExecutionId", jobExecutionData.getExecutionId());
        System.out.printf("uri: %s%n", uri);
        final JobInstanceData data = target.request().get(JobInstanceData.class);

        System.out.printf("Got JobInstanceData: %s%n", data);
        assertEquals(jobName1, data.getJobName());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Test
    public void getJobExecution() throws Exception {
        final JobExecutionData jobExecutionData = startJob(jobName1, null);
        final URI uri = getJobExecutionUriBuilder(null).path(String.valueOf(jobExecutionData.getExecutionId())).build();
        System.out.printf("uri: %s%n", uri);
        final WebTarget target = client.target(uri);
        final JobExecutionData data = target.request().get(JobExecutionData.class);

        System.out.printf("Got JobExecutionData: %s%n", data);
        assertEquals(jobName1, data.getJobName());
    }

    @Test
    public void getJobExecutionBadId() throws Exception {
        final URI uri = getJobExecutionUriBuilder(null).path(String.valueOf(Long.MAX_VALUE)).build();
        System.out.printf("uri: %s%n", uri);
        final WebTarget target = client.target(uri);
        final Response response = target.request().get();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void abandon() throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty("fail", String.valueOf(true));
        final JobExecutionData jobExecutionData = startJob(jobWithParams, queryParams);

        Thread.sleep(500);
        final URI uri = getJobExecutionUriBuilder("abandon")
                .resolveTemplate("jobExecutionId", String.valueOf(jobExecutionData.getExecutionId())).build();
        System.out.printf("uri: %s%n", uri);
        final WebTarget target = client.target(uri);
        final Response response = target.request().post(null);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        //abandon it again (should be idempotent)
        final Response response2 = client.target(uri).request().post(null);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response2.getStatus());

        final URI abandonedJobExecutionUri = getJobExecutionUriBuilder(null).
                path(String.valueOf(jobExecutionData.getExecutionId())).build();
        System.out.printf("uri: %s%n", abandonedJobExecutionUri);
        final WebTarget jobExecutionTarget = client.target(abandonedJobExecutionUri);
        final JobExecutionData data = jobExecutionTarget.request().get(JobExecutionData.class);
        assertEquals(BatchStatus.ABANDONED, data.getBatchStatus());
    }

    @Test
    public void restart() throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty("fail", String.valueOf(true));
        JobExecutionData jobExecution = startJob(jobWithParams, queryParams);
        assertEquals(queryParams, jobExecution.getJobParameters());

        Thread.sleep(500);
        queryParams.setProperty("fail", String.valueOf(false));
        final JobExecutionData restartJobExecution = restartJobExecution(jobExecution.getExecutionId(), queryParams);
        assertEquals(queryParams, restartJobExecution.getJobParameters());

        Thread.sleep(500);
        jobExecution = getJobExecution(restartJobExecution.getExecutionId());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(jobWithParams, jobExecution.getJobName());
    }

    @Test
    public void stop() throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty("sleepMillis", String.valueOf(1000));
        queryParams.setProperty("fail", String.valueOf(false));
        JobExecutionData jobExecution = startJob(jobWithParams, queryParams);
        assertEquals(queryParams, jobExecution.getJobParameters());

        final URI uri = getJobExecutionUriBuilder("stop").resolveTemplate("jobExecutionId", jobExecution.getExecutionId()).build();
        final WebTarget target = client.target(uri);
        System.out.printf("uri: %s%n", uri);
        final Response response = target.request().post(emptyJsonEntity());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        Thread.sleep(1000);
        final JobExecutionData jobExecutionStopped = getJobExecution(jobExecution.getExecutionId());
        assertEquals(BatchStatus.STOPPED, jobExecutionStopped.getBatchStatus());
        assertEquals(jobWithParams, jobExecutionStopped.getJobName());
    }

    @Test
    public void getRunningExecutions() throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty("sleepMillis", String.valueOf(2000));
        JobExecutionData jobExecution1 = startJob(jobWithParams, queryParams);
        JobExecutionData jobExecution2 = startJob(jobWithParams, queryParams);

        final URI uri = getJobExecutionUriBuilder("getRunningExecutions").build();
        queryParams.clear();
        queryParams.setProperty("jobName", jobWithParams);
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        final JobExecutionData[] jobExecutionData = target.request().get(JobExecutionData[].class);
        assertEquals(2, jobExecutionData.length);
    }

    @Test
    public void getRunningExecutionsEmpty() throws Exception {
        final Properties queryParams = new Properties();
        JobExecutionData jobExecution1 = startJob(jobWithParams, null);
        JobExecutionData jobExecution2 = startJob(jobWithParams, null);

        Thread.sleep(500);
        final URI uri = getJobExecutionUriBuilder("getRunningExecutions").build();
        queryParams.setProperty("jobName", jobWithParams);
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        final JobExecutionData[] data = target.request().get(JobExecutionData[].class);
        assertEquals(0, data.length);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getStepExecutions() throws Exception {
        JobExecutionData jobExecution1 = startJob(jobWithParams, null);

        Thread.sleep(500);
        final StepExecutionData[] data = getStepExecutions(jobExecution1.getExecutionId());
        assertEquals(1, data.length);
        assertEquals(BatchStatus.COMPLETED, data[0].getBatchStatus());
        assertEquals(jobWithParams + ".step1", data[0].getStepName());
        System.out.printf("Got step metrics: %s%n", Arrays.toString(data[0].getMetrics()));
    }

    @Test
    public void getStepExecution() throws Exception {
        JobExecutionData jobExecution1 = startJob(jobWithParams, null);

        Thread.sleep(500);
        final StepExecutionData[] data = getStepExecutions(jobExecution1.getExecutionId());
        final long stepExecutionId = data[0].getStepExecutionId();
        final URI uri = getJobExecutionUriBuilder("getStepExecution")
                .resolveTemplate("jobExecutionId", jobExecution1.getExecutionId())
                .resolveTemplate("stepExecutionId", stepExecutionId).build();
        WebTarget target = getTarget(uri, null);
        System.out.printf("uri: %s%n", uri);
        final StepExecutionData stepExecutionData = target.request().get(StepExecutionData.class);
        assertEquals(BatchStatus.COMPLETED, stepExecutionData.getBatchStatus());
        assertEquals(jobWithParams + ".step1", stepExecutionData.getStepName());
        System.out.printf("Got step metrics: %s%n", Arrays.toString(stepExecutionData.getMetrics()));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private JobExecutionData startJob(final String jobXmlName, final Properties queryParams) throws Exception {
        final URI uri = getJobUriBuilder("start").resolveTemplate("jobXmlName", jobXmlName).build();
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        return target.request().post(emptyJsonEntity(), JobExecutionData.class);
    }

    private JobExecutionData restartJobExecution(final long jobExecutionId, final Properties queryParams) throws Exception {
        final URI uri = getJobExecutionUriBuilder("restart").resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        return target.request().post(emptyJsonEntity(), JobExecutionData.class);
    }

    private WebTarget getTarget(final URI uri, final Properties props) {
        WebTarget result = client.target(uri);

        if (props == null) {
            return result;
        } else {
            for (final String k : props.stringPropertyNames()) {
                result = result.queryParam(k, props.getProperty(k));
            }
        }
        return result;
    }

    private JobExecutionData getJobExecution(final long jobExecutionId) {
        final URI uri = getJobExecutionUriBuilder(null).path(String.valueOf(jobExecutionId)).build();
        System.out.printf("uri: %s%n", uri);
        final WebTarget target = client.target(uri);
        return target.request().get(JobExecutionData.class);
    }

    private StepExecutionData[] getStepExecutions(final long jobExecutionId) {
        final URI uri = getJobExecutionUriBuilder("getStepExecutions")
                .resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = getTarget(uri, null);
        System.out.printf("uri: %s%n", uri);
        return target.request().get(StepExecutionData[].class);
    }

    private Entity<Object> emptyJsonEntity() {
        return Entity.entity(null, MediaType.APPLICATION_JSON_TYPE);
    }

    private UriBuilder getJobUriBuilder(final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(restUrl).path(JobResource.class);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(JobResource.class, methodName);
        }
        return uriBuilder;
    }

    private UriBuilder getJobInstanceUriBuilder(final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(restUrl).path(JobInstanceResource.class);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(JobInstanceResource.class, methodName);
        }
        return uriBuilder;
    }

    private UriBuilder getJobExecutionUriBuilder(final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(restUrl).path(JobExecutionResource.class);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(JobExecutionResource.class, methodName);
        }
        return uriBuilder;
    }
}