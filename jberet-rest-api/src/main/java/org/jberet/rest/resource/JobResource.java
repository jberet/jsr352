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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jberet.rest.entity.JobExecutionEntity;

@Path("jobs")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobResource {

    @Path("{jobXmlName}/start")
    @POST
    public Response start(final @PathParam("jobXmlName") String jobXmlName,
                          final @Context UriInfo uriInfo,
                          final Properties jobParamsAsProps) {
        JobExecutionEntity jobExecutionData = JobService.getInstance()
                .start(jobXmlName, jobParametersFromUriInfoAndProps(uriInfo, jobParamsAsProps));
        final URI jobExecutionDataUri = uriInfo.getBaseUriBuilder().path(JobExecutionResource.class).
                path(String.valueOf(jobExecutionData.getExecutionId())).
                build();
        return Response.created(jobExecutionDataUri).entity(jobExecutionData).build();
    }

    @GET
    public Response getJobNames() {
        final Set<String> jobNames = JobService.getInstance().getJobNames();
        final String[] jobNamesArray = jobNames.toArray(new String[jobNames.size()]);
//        final GenericEntity<Set<String>> entity = new GenericEntity<Set<String>>(jobNames) {
//        };
        return Response.ok(jobNamesArray).build();
    }

    static Properties jobParametersFromUriInfoAndProps(final UriInfo uriInfo, final Properties props) {
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters(true);
        if (queryParameters.isEmpty()) {
            return props;
        }

        final Properties p = new Properties();
        for (final Map.Entry<String, List<String>> e : queryParameters.entrySet()) {
            p.setProperty(e.getKey(), e.getValue().get(0));
        }
        if (props != null) {
            for (final String k : props.stringPropertyNames()) {
                p.setProperty(k, props.getProperty(k));
            }
        }
        return p;
    }

}
