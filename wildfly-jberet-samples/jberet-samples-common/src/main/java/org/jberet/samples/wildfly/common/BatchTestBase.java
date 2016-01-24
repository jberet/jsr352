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
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.StepExecutionEntity;
import org.jberet.rest.resource.JobExecutionResource;
import org.jberet.rest.resource.JobInstanceResource;
import org.jberet.rest.resource.JobResource;
import org.junit.Assert;

/**
 * Base class to be extended by concrete batch test cases.
 */
public abstract class BatchTestBase {
    protected static final String BASE_URL = "http://localhost:8080/";

    protected Client client = ClientBuilder.newClient();

    /**
     * Gets the full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     *
     * @return the full REST API URL
     */
    protected abstract String getRestUrl();

    protected void startJobShouldComplete(final String jobName, final Properties queryParams, final long waitMillis)
            throws Exception {
        final JobExecutionEntity jobExecution = startJob(jobName, queryParams);
        Thread.sleep(waitMillis);
        final JobExecutionEntity jobExecution2 = getJobExecution(jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution2.getBatchStatus());
    }

    protected JobExecutionEntity startJob(final String jobXmlName, final Properties queryParams) throws Exception {
        final URI uri = getJobUriBuilder("start").resolveTemplate("jobXmlName", jobXmlName).build();
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        return target.request().post(emptyJsonEntity(), JobExecutionEntity.class);
    }

    protected JobExecutionEntity restartJobExecution(final long jobExecutionId, final Properties queryParams) throws Exception {
        final URI uri = getJobExecutionUriBuilder("restart").resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = getTarget(uri, queryParams);
        System.out.printf("uri: %s%n", uri);
        return target.request().post(emptyJsonEntity(), JobExecutionEntity.class);
    }

    protected WebTarget getTarget(final URI uri, final Properties props) {
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

    protected JobExecutionEntity getJobExecution(final long jobExecutionId) {
        final URI uri = getJobExecutionUriBuilder(null).path(String.valueOf(jobExecutionId)).build();
        System.out.printf("uri: %s%n", uri);
        final WebTarget target = client.target(uri);
        return target.request().get(JobExecutionEntity.class);
    }

    protected StepExecutionEntity[] getStepExecutions(final long jobExecutionId) {
        final URI uri = getJobExecutionUriBuilder("getStepExecutions")
                .resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = getTarget(uri, null);
        System.out.printf("uri: %s%n", uri);
        return target.request().get(StepExecutionEntity[].class);
    }

    protected Entity<Object> emptyJsonEntity() {
        return Entity.entity(null, MediaType.APPLICATION_JSON_TYPE);
    }

    protected UriBuilder getJobUriBuilder(final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(getRestUrl()).path(JobResource.class);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(JobResource.class, methodName);
        }
        return uriBuilder;
    }

    protected UriBuilder getJobInstanceUriBuilder(final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(getRestUrl()).path(JobInstanceResource.class);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(JobInstanceResource.class, methodName);
        }
        return uriBuilder;
    }

    protected UriBuilder getJobExecutionUriBuilder(final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(getRestUrl()).path(JobExecutionResource.class);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(JobExecutionResource.class, methodName);
        }
        return uriBuilder;
    }
}
