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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jberet.rest._private.RestAPIMessages;
import org.jberet.rest.entity.JobInstanceEntity;

@Path("/jobinstances")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobInstanceResource {

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

        if (jobName == null) {
            throw RestAPIMessages.MESSAGES.missingQueryParams("jobName");
        }
        if (start < 0) {
            throw RestAPIMessages.MESSAGES.invalidQueryParamValue("start", String.valueOf(start));
        }
        if (count < 0) {
            throw RestAPIMessages.MESSAGES.invalidQueryParamValue("count", String.valueOf(count));
        }
        final List<JobInstanceEntity> jobInstanceData = JobService.getInstance().getJobInstances(jobName, start, count);
        return Response.ok(jobInstanceData.toArray(new JobInstanceEntity[jobInstanceData.size()])).build();
    }

    @Path("/count")
    @GET
    public int getJobInstanceCount(final @QueryParam("jobName") String jobName) {
        if (jobName == null) {
            throw RestAPIMessages.MESSAGES.missingQueryParams("jobName");
        }
        return JobService.getInstance().getJobInstanceCount(jobName);
    }
}
