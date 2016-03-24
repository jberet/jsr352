/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduler;

/**
 * REST resource class for batch job schedules.
 *
 * @since 1.3.0
 */
@Path("schedules")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobScheduleResource {
    @GET
    @Path("")
    public JobSchedule[] getJobSchedules() {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        final List<JobSchedule> jobScheduleList = jobScheduler.getJobSchedules();
        return jobScheduleList.toArray(new JobSchedule[jobScheduleList.size()]);
    }

    @POST
    @Path("{scheduleId}/cancel")
    public boolean cancel(final @PathParam("scheduleId") String scheduleId) {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        return jobScheduler.cancel(scheduleId);
    }

    @GET
    @Path("{scheduleId}")
    public JobSchedule getJobSchedule(final @PathParam("scheduleId") String scheduleId) {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        return jobScheduler.getJobSchedule(scheduleId);
    }
}
