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

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jberet.rest.model.JobExecutionData;

@Path("jobexecutions")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobExecutionResource {

    @Path("running")
    @GET
    public Response getRunningExecutions(final @QueryParam("jobName") String jobName) {
        final List<JobExecutionData> runningExecutions = JobService.getInstance().getRunningExecutions(jobName);
        return Response.ok(runningExecutions.toArray(new JobExecutionData[runningExecutions.size()])).build();
    }

    @Path("{jobExecutionId}")
    @GET
    public Response getJobExecution(final @PathParam("jobExecutionId") long jobExecutionId) {
        final JobExecutionData jobExecutionData = JobService.getInstance().getJobExecution(jobExecutionId);
        return Response.ok(jobExecutionData).build();
    }

    @Path("{jobExecutionId}/abandon")
    @PUT
    public void abandon(final @PathParam("jobExecutionId") long jobExecutionId) {
        JobService.getInstance().abandon(jobExecutionId);
    }

    @Path("{jobExecutionId}/stop")
    @PUT
    public void stop(final @PathParam("jobExecutionId") long jobExecutionId) {
        JobService.getInstance().stop(jobExecutionId);
    }

    @Path("{jobExecutionId}/restart")
    @POST
    public JobExecutionData restart(final @PathParam("jobExecutionId") long jobExecutionId,
                                    final @Context UriInfo uriInfo) {
        return JobService.getInstance().restart(jobExecutionId, JobResource.jobParametersFromUriInfo(uriInfo));
    }

}
