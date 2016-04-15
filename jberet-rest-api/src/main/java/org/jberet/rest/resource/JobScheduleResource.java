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

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
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
    /**
     * Gets all job schedules.
     *
     * @return all job schedules as array
     */
    @GET
    @Path("")
    public JobSchedule[] getJobSchedules() {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        final List<JobSchedule> jobScheduleList = jobScheduler.getJobSchedules();
        return jobScheduleList.toArray(new JobSchedule[jobScheduleList.size()]);
    }

    /**
     * Cancels a job schedule.
     *
     * @param scheduleId the job schedule id to cancel
     * @return true if the job schedule is cancelled successfully; false otherwise
     */
    @POST
    @Path("{scheduleId}/cancel")
    public boolean cancel(final @PathParam("scheduleId") String scheduleId) {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        return jobScheduler.cancel(scheduleId);
    }

    /**
     * Gets a job schedule by its id.
     *
     * @param scheduleId the job schedule id to get
     * @return the job schedule, and null if the job schedule is not found
     */
    @GET
    @Path("{scheduleId}")
    public JobSchedule getJobSchedule(final @PathParam("scheduleId") String scheduleId) {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        return jobScheduler.getJobSchedule(scheduleId);
    }

    /**
     * Gets all available timezone ids, and the first element of the result is the default timezone id.
     *
     * @return timezone ids as a string array
     */
    @GET
    @Path("timezones")
    public String[] getTimezoneIds() {
        final String[] availableIDs = TimeZone.getAvailableIDs();
        Arrays.sort(availableIDs);
        final int i = Arrays.binarySearch(availableIDs, TimeZone.getDefault().getID());
        final String[] result = new String[availableIDs.length];
        result[0] = availableIDs[i];
        System.arraycopy(availableIDs, 0, result, 1, i);
        System.arraycopy(availableIDs, i + 1, result, i + 1, availableIDs.length - (i + 1));

        return result;
    }

    /**
     * Gets the scheduling features supported by the current job scheduler.
     *
     * @return supported features as a string array
     */
    @GET
    @Path("features")
    public String[] getFeatures() {
        return JobScheduler.getJobScheduler().getFeatures();
    }
}

