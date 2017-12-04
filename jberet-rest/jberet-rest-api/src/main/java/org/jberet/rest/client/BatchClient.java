/*
 * Copyright (c) 2016-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.client;

import java.net.URI;
import java.util.Properties;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.JobInstanceEntity;
import org.jberet.rest.entity.StepExecutionEntity;
import org.jberet.rest.resource.JobExecutionResource;
import org.jberet.rest.resource.JobInstanceResource;
import org.jberet.rest.resource.JobResource;
import org.jberet.rest.resource.JobScheduleResource;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;

/**
 * Common batch client operations via REST API.
 *
 * @since 1.3.0
 */
public class BatchClient {
    private final Client client;
    private final String restUrl;

    /**
     * Constructs {@code BatchClient} with the specified REST URL.
     *
     * @param restUrl REST URL, for example, http://localhost:8080/app1/api
     */
    public BatchClient(final String restUrl) {
        this(ClientBuilder.newClient(), restUrl);
    }

    /**
     * Constructs {@code BatchClient} with the specified {@code javax.ws.rs.client.Client}
     * and REST URL.
     *
     * @param client {@code javax.ws.rs.client.Client}
     * @param restUrl REST URL, for example, http://localhost:8080/app1/api
     */
    public BatchClient(final Client client, final String restUrl) {
        this.client = client;
        this.restUrl = restUrl;
    }

    /**
     * Gets the {@code javax.ws.rs.client.Client} associated with this {@code BatchClient}.
     * @return the {@code javax.ws.rs.client.Client}
     */
    public Client getClient() {
        return client;
    }

    /**
     * Gets the REST URL for this {@code BatchClient}.
     * @return REST URL, for example, http://localhost:8080/app1/api
     */
    public String getRestUrl() {
        return restUrl;
    }

    /**
     * Starts the job specified by the job XML name and job parameters.
     * @param jobXmlName job XML name for the job to start
     * @param queryParams job parameters
     * @return the new job execution entity
     * @throws Exception if errors occur
     */
    public JobExecutionEntity startJob(final String jobXmlName, final Properties queryParams) throws Exception {
        final URI uri = getJobUriBuilder("start").resolveTemplate("jobXmlName", jobXmlName).build();
        WebTarget target = target(uri, queryParams);
        return target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), JobExecutionEntity.class);
    }

    /**
     * Restarts the job execution specified by the job execution id and job parameters.
     *
     * @param jobExecutionId job execution id
     * @param queryParams job parameters
     * @return the new job execution entity
     * @throws Exception if errors occur
     */
    public JobExecutionEntity restartJobExecution(final long jobExecutionId, final Properties queryParams) throws Exception {
        final URI uri = getJobExecutionUriBuilder("restart").resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = target(uri, queryParams);
        return target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), JobExecutionEntity.class);
    }

    /**
     * Stops the job execution specified by the job execution id.
     *
     * @param jobExecutionId job execution id
     * @throws Exception if errors occur
     */
    public void stopJobExecution(final long jobExecutionId) throws Exception {
        final URI uri = getJobExecutionUriBuilder("stop").resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = target(uri, null);
        target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Restarts the latest failed or stopped job execution belonging to the
     * specified job name.
     * This method is equivalent to first finding the latest failed or stopped
     * job execution belonging to the job name, and then restarting that job
     * execution id.
     *
     * @param jobXmlName job name (id)
     * @param queryParams job parameters
     * @return the new job execution entity
     * @throws Exception if errors occur
     */
    public JobExecutionEntity restartJobExecution(final String jobXmlName, final Properties queryParams) throws Exception {
        final URI uri = getJobUriBuilder("restart").resolveTemplate("jobXmlName", jobXmlName).build();
        WebTarget target = target(uri, queryParams);
        return target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), JobExecutionEntity.class);
    }

    /**
     * Gets all job instances belonging to the specified job name,
     * in reverse chronological order.
     *
     * @param jobName the job name
     * @param start the relative starting number (zero based) to
     *            return from the maximal list of job instances
     * @param count the number of job instances to return from the
     *              starting position of the maximal list of job instances
     * @return matching job instances
     * @throws Exception if errors occur
     */
    public JobInstanceEntity[] getJobInstances(final String jobName, final int start, final int count)
            throws Exception {
        final URI uri = getJobInstanceUriBuilder(null).build();
        final WebTarget target = target(uri)
                .queryParam("jobName", jobName)
                .queryParam("start", start)
                .queryParam("count", count);
        return target.request().get(JobInstanceEntity[].class);
    }

    /**
     * Gets the {@code javax.ws.rs.client.WebTarget} for the specified URI.
     *
     * @param uri uri
     * @return {@code javax.ws.rs.client.WebTarget} for the uri
     */
    public WebTarget target(final URI uri) {
        return client.target(uri);
    }

    /**
     * Gets the {@code javax.ws.rs.client.WebTarget} for the specified URI and
     * query parameters.
     *
     * @param uri uri
     * @param props query parameters
     * @return {@code javax.ws.rs.client.WebTarget}
     */
    public WebTarget target(final URI uri, final Properties props) {
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

    /**
     * Gets the job execution entity for the specified job execution id.
     * @param jobExecutionId job execution id
     * @return job execution entity
     */
    public JobExecutionEntity getJobExecution(final long jobExecutionId) {
        final URI uri = getJobExecutionUriBuilder(null).path(String.valueOf(jobExecutionId)).build();
        final WebTarget target = client.target(uri);
        return target.request().get(JobExecutionEntity.class);
    }

    /**
     * Gets all step execution entities for the specified job execution id.
     * @param jobExecutionId job execution id
     * @return all step execution entities
     */
    public StepExecutionEntity[] getStepExecutions(final long jobExecutionId) {
        final URI uri = getJobExecutionUriBuilder("getStepExecutions")
                .resolveTemplate("jobExecutionId", jobExecutionId).build();
        WebTarget target = target(uri);
        return target.request().get(StepExecutionEntity[].class);
    }

    /**
     * Gets the job schedule for the specified job schedule id.
     * @param scheduleId job schedule id
     * @return job schedule
     */
    public JobSchedule getJobSchedule(final String scheduleId) {
        final URI uri = getJobScheduleUriBuilder("getJobSchedule")
                .resolveTemplate("scheduleId", scheduleId).build();
        WebTarget target = target(uri);
        return target.request().accept(MediaType.APPLICATION_JSON_TYPE).get(JobSchedule.class);
    }

    /**
     * Gets all job schedules for the current job scheduler.
     * @return all job schedules
     */
    public JobSchedule[] getJobSchedules() {
        final URI uri = getJobScheduleUriBuilder("getJobSchedules").build();
        final WebTarget target = target(uri);
        return target.request().accept(MediaType.APPLICATION_JSON_TYPE).get(JobSchedule[].class);
    }

    /**
     * Gets all feature names supported by the current job scheduler.
     * @return all feature names as a string array
     */
    public String[] getJobScheduleFeatures() {
        final URI uri = getJobScheduleUriBuilder("getFeatures").build();
        final WebTarget target = target(uri);
        return target.request().accept(MediaType.APPLICATION_JSON_TYPE).get(String[].class);
    }

    /**
     * Cancels the job schedule specified by the job schedule id.
     * @param scheduleId job schedule id
     * @return true if successfully cancelled; false otherwise
     */
    public boolean cancelJobSchedule(final String scheduleId) {
        final URI uri = getJobScheduleUriBuilder("cancel")
                .resolveTemplate("scheduleId", scheduleId).build();
        WebTarget target = target(uri);
        return target.request().accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), boolean.class);
    }

    /**
     * Deletes the job schedule specified by the job schedule id.
     * @param scheduleId job schedule id
     *
     * @since 1.3.0.Beta7
     */
    public void deleteJobSchedule(final String scheduleId) {
        final URI uri = getJobScheduleUriBuilder("delete")
                .resolveTemplate("scheduleId", scheduleId).build();
        WebTarget target = target(uri);
        target.request().delete();
    }

    /**
     * Submits a job schedule as specified by the job schedule config.
     * @param scheduleConfig job schedule config
     * @return the new job schedule from the submission
     */
    public JobSchedule schedule(final JobScheduleConfig scheduleConfig) {
        final URI uri;
        if (scheduleConfig.getJobName() != null) {
            uri = getJobUriBuilder("schedule").resolveTemplate("jobXmlName", scheduleConfig.getJobName()).build();
        } else {
            uri = getJobExecutionUriBuilder("schedule")
                    .resolveTemplate("jobExecutionId", scheduleConfig.getJobExecutionId()).build();
        }
        WebTarget target = target(uri);
        return target.request().post(Entity.json(scheduleConfig), JobSchedule.class);
    }

    /**
     * Gets the {@code javax.ws.rs.core.UriBuilder} for the specified
     * REST resource class and method.
     *
     * @param cls REST resource class name
     * @param methodName name of the resource method in the above REST resource class
     * @return {@code javax.ws.rs.core.UriBuilder}
     */
    public UriBuilder getUriBuilder(final Class<?> cls, final String methodName) {
        UriBuilder uriBuilder = UriBuilder.fromPath(restUrl).path(cls);
        if (methodName != null) {
            uriBuilder = uriBuilder.path(cls, methodName);
        }
        return uriBuilder;
    }

    /**
     * Gets the {@code javax.ws.rs.core.UriBuilder} for the specified method
     * name in {@link JobResource} class.
     *
     * @param methodName method name in {@code JobResource} class
     * @return {@code javax.ws.rs.core.UriBuilder}
     */
    public UriBuilder getJobUriBuilder(final String methodName) {
        return getUriBuilder(JobResource.class, methodName);
    }

    /**
     * Gets the {@code javax.ws.rs.core.UriBuilder} for the specified method
     * name in {@link JobInstanceResource} class.
     *
     * @param methodName method name in {@code JobInstanceResource} class
     * @return {@code javax.ws.rs.core.UriBuilder}
     */
    public UriBuilder getJobInstanceUriBuilder(final String methodName) {
        return getUriBuilder(JobInstanceResource.class, methodName);
    }

    /**
     * Gets the {@code javax.ws.rs.core.UriBuilder} for the specified method
     * name in {@link JobExecutionResource} class.
     *
     * @param methodName method name in {@code JobExecutionResource} class
     * @return {@code javax.ws.rs.core.UriBuilder}
     */
    public UriBuilder getJobExecutionUriBuilder(final String methodName) {
        return getUriBuilder(JobExecutionResource.class, methodName);
    }

    /**
     * Gets the {@code javax.ws.rs.core.UriBuilder} for the specified method
     * name in {@link JobScheduleResource} class.
     *
     * @param methodName method name in {@code JobScheduleResource} class
     * @return {@code javax.ws.rs.core.UriBuilder}
     */
    public UriBuilder getJobScheduleUriBuilder(final String methodName) {
        return getUriBuilder(JobScheduleResource.class, methodName);
    }

}
