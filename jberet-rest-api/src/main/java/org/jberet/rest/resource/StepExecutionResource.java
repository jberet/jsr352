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

import org.jberet.rest.model.StepExecutionData;

@Path("stepexecutions")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class StepExecutionResource {

    @GET
    @Path("/")
    public Response getStepExecutions(final @QueryParam("jobExecutionId") long jobExecutionId) {
        final List<StepExecutionData> stepExecutionData = JobService.getInstance().getStepExecutions(jobExecutionId);
        return Response.ok(stepExecutionData.toArray(new StepExecutionData[stepExecutionData.size()])).build();
    }
}
