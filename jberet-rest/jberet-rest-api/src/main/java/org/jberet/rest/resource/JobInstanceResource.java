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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jberet.rest._private.RestAPIMessages;
import org.jberet.rest.entity.JobInstanceEntity;
import org.jberet.rest.service.JobService;

/**
 * REST resource class for job instance. This class supports job-instance-related
 * operations such as listing job instances for a job name/id, getting job instance
 * for a job execution, and counting job instances for a job name/id.
 *
 * @since 1.3.0
 */
@Path("/jobinstances")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobInstanceResource {
    /**
     * Gets job instance(s) for a job name/id, or job execution.
     * If {@code jobExecutionId} is present in query parameters, returns
     * the response with job instance ({@code org.jberet.rest.entity.JobInstanceEntity}) for
     * the job execution id.
     * Otherwise, returns the response with job instances
     * ({@code org.jberet.rest.entity.JobInstanceEntity[]} matching the specified
     * {@code jobName}, {@code start} and {@code count}.
     *
     * @param jobName job name/id for which to get job instances
     * @param start the offset position in the list of all eligible job instances to include
     * @param count limit the number of job instances in the result
     * @param jobExecutionId the job execution id for which to get job instance

     * @return {@code javax.ws.rs.core.Response} including response status, and
     * response entity ({@code org.jberet.rest.entity.JobInstanceEntity} for job execution id,
     * and {@code org.jberet.rest.entity.JobInstanceEntity[]} for job name/id.
     */
    @GET
    public Response getJobInstances(final @QueryParam("jobName") String jobName,
                                    final @QueryParam("start") int start,
                                    final @QueryParam("count") int count,
                                    final @QueryParam("jobExecutionId") long jobExecutionId) {
        if (jobExecutionId > 0) {
            final JobInstanceEntity jobInstanceData = JobService.getInstance().getJobInstance(jobExecutionId);
            return Response.ok(jobInstanceData).build();
        } else if (jobExecutionId < 0) {
            throw RestAPIMessages.MESSAGES.invalidQueryParamValue("jobExecutionId", String.valueOf(jobExecutionId));
        }

        //if jobName is null, treat it as "*"
        //if count is not set, treat it as Integer.MAX
        if (start < 0) {
            throw RestAPIMessages.MESSAGES.invalidQueryParamValue("start", String.valueOf(start));
        }
        if (count < 0) {
            throw RestAPIMessages.MESSAGES.invalidQueryParamValue("count", String.valueOf(count));
        }
        final JobInstanceEntity[] jobInstanceData =
                JobService.getInstance().getJobInstances(jobName == null ? "*" : jobName, start,
                        count == 0 ? Integer.MAX_VALUE : count);

        return Response.ok(jobInstanceData).build();
    }

    /**
     * Gets the number of job instances for the specified job name/id.
     *
     * @param jobName the job name/id to count its job instances
     * @return the number of job instances for {@code jobName}
     */
    @Path("/count")
    @GET
    public int getJobInstanceCount(final @QueryParam("jobName") String jobName) {
        if (jobName == null) {
            throw RestAPIMessages.MESSAGES.missingQueryParams("jobName");
        }
        return JobService.getInstance().getJobInstanceCount(jobName);
    }
}
