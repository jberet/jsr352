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

package org.jberet.rest.resource;

import java.util.Properties;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jberet.rest._private.RestAPIMessages;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.StepExecutionEntity;

@Path("jobexecutions")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobExecutionResource {

    @GET
    public JobExecutionEntity[] getJobExecutions(final @QueryParam("jobInstanceId") long jobInstanceId,
                                                 final @QueryParam("jobExecutionId1") long jobExecutionId1,
                                                 final @Context UriInfo uriInfo) {
        final JobExecutionEntity[] jobExecutionEntities = JobService.getInstance().getJobExecutions(jobInstanceId, jobExecutionId1);
        setJobExecutionEntityHref(uriInfo, jobExecutionEntities);
        return jobExecutionEntities;
    }

    @Path("running")
    @GET
    public JobExecutionEntity[] getRunningExecutions(final @QueryParam("jobName") String jobName,
                                                     final @Context UriInfo uriInfo) {
        final JobExecutionEntity[] jobExecutionEntities = JobService.getInstance().getRunningExecutions(jobName);
        setJobExecutionEntityHref(uriInfo, jobExecutionEntities);
        return jobExecutionEntities;
    }

    @Path("{jobExecutionId}")
    @GET
    public JobExecutionEntity getJobExecution(final @PathParam("jobExecutionId") long jobExecutionId,
                                              final @Context UriInfo uriInfo) {
        final JobExecutionEntity jobExecution = JobService.getInstance().getJobExecution(jobExecutionId);
        setJobExecutionEntityHref(uriInfo, jobExecution);
        return jobExecution;
    }

    @Path("{jobExecutionId}/abandon")
    @POST
    public void abandon(final @PathParam("jobExecutionId") long jobExecutionId) {
        JobService.getInstance().abandon(jobExecutionId);
    }

    @Path("{jobExecutionId}/stop")
    @POST
    public void stop(final @PathParam("jobExecutionId") long jobExecutionId) {
        JobService.getInstance().stop(jobExecutionId);
    }

    @Path("{jobExecutionId}/restart")
    @POST
    public JobExecutionEntity restart(final @PathParam("jobExecutionId") long jobExecutionId,
                                      final @Context UriInfo uriInfo,
                                      final Properties jobParamsAsProps) {
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().restart(
                jobExecutionId, JobResource.jobParametersFromUriInfoAndProps(uriInfo, jobParamsAsProps));
        setJobExecutionEntityHref(uriInfo, jobExecutionEntity);
        return jobExecutionEntity;
    }

    @GET
    @Path("{jobExecutionId}/stepexecutions")
    public StepExecutionEntity[] getStepExecutions(final @PathParam("jobExecutionId") long jobExecutionId) {
        return JobService.getInstance().getStepExecutions(jobExecutionId);
    }

    @GET
    @Path("{jobExecutionId}/stepexecutions/{stepExecutionId}")
    public StepExecutionEntity getStepExecution(final @PathParam("jobExecutionId") long jobExecutionId,
                                                final @PathParam("stepExecutionId") long stepExecutionId) {
        final StepExecutionEntity[] stepExecutionData = JobService.getInstance().getStepExecutions(jobExecutionId);
        for (final StepExecutionEntity e : stepExecutionData) {
            if (e.getStepExecutionId() == stepExecutionId) {
                return e;
            }
        }
        throw RestAPIMessages.MESSAGES.notFoundException("stepExecutionId", String.valueOf(stepExecutionId));
    }

    static void setJobExecutionEntityHref(final UriInfo uriInfo, final JobExecutionEntity... entities) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(JobExecutionResource.class);
        for (final JobExecutionEntity e : entities) {
            e.setHref(uriBuilder.clone().path(String.valueOf(e.getExecutionId())).build().toString());
        }
    }

}
