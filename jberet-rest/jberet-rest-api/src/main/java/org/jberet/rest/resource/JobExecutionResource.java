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
import org.jberet.rest.service.JobService;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduler;

/**
 * REST resource class for job execution. This class supports job-execution- and
 * step-execution-related operations.
 *
 * @since 1.3.0
 */
@Path("jobexecutions")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JobExecutionResource {
    /**
     * Gets the job executions for a job instance. The number of results may be
     * limited by {@code count} query parameter.
     *
     * @param count the maximum number of matching job executions
     * @param jobInstanceId job instance id
     * @param jobExecutionId1 id of any job execution belonging to the target job instance
     * @param uriInfo {@code javax.ws.rs.core.UriInfo}
     *
     * @return job executions matching the job instance
     */
    @GET
    public JobExecutionEntity[] getJobExecutions(final @QueryParam("count") int count,
                                                 final @QueryParam("jobInstanceId") long jobInstanceId,
                                                 final @QueryParam("jobExecutionId1") long jobExecutionId1,
                                                 final @Context UriInfo uriInfo) {
        //jobExecutionId1 is used to retrieve the JobInstance, from which to get all its JobExecution's
        //jobInstanceId param is currently not used.
        final JobExecutionEntity[] jobExecutionEntities = JobService.getInstance().getJobExecutions(count, jobInstanceId, jobExecutionId1);
        setJobExecutionEntityHref(uriInfo, jobExecutionEntities);
        return jobExecutionEntities;
    }

    /**
     * Gets the running job executions for a job name/id.
     *
     * @param jobName the job name/id
     * @param uriInfo {@code javax.ws.rs.core.UriInfo}
     * @return running job executions for {@code jobName}
     */
    @Path("running")
    @GET
    public JobExecutionEntity[] getRunningExecutions(final @QueryParam("jobName") String jobName,
                                                     final @Context UriInfo uriInfo) {
        final JobExecutionEntity[] jobExecutionEntities = JobService.getInstance().getRunningExecutions(jobName);
        setJobExecutionEntityHref(uriInfo, jobExecutionEntities);
        return jobExecutionEntities;
    }

    /**
     * Gets the job execution by its id.
     *
     * @param jobExecutionId job execution id
     * @param uriInfo {@code javax.ws.rs.core.UriInfo}
     *
     * @return the job execution with {@code jobExecutionId}
     */
    @Path("{jobExecutionId}")
    @GET
    public JobExecutionEntity getJobExecution(final @PathParam("jobExecutionId") long jobExecutionId,
                                              final @Context UriInfo uriInfo) {
        final JobExecutionEntity jobExecution = JobService.getInstance().getJobExecution(jobExecutionId);
        setJobExecutionEntityHref(uriInfo, jobExecution);
        return jobExecution;
    }

    /**
     * Abandons a job execution with a particular id.
     *
     * @param jobExecutionId job execution id
     */
    @Path("{jobExecutionId}/abandon")
    @POST
    public void abandon(final @PathParam("jobExecutionId") long jobExecutionId) {
        JobService.getInstance().abandon(jobExecutionId);
    }

    /**
     * Stops a job execution with a particular id.
     *
     * @param jobExecutionId job execution id
     */
    @Path("{jobExecutionId}/stop")
    @POST
    public void stop(final @PathParam("jobExecutionId") long jobExecutionId) {
        JobService.getInstance().stop(jobExecutionId);
    }

    /**
     * Restarts a job execution with a particular id, and optional job parameters.
     * <p>
     * Job parameters can be taken from query parameters, obtained from {@code uriInfo},
     * or {@code jobParamsAsProps} as {@code java.util.Properties}, or both.
     * When extracting query parameters from {@code uriInfo}, only the first value of
     * each key is used. When a key exists in both query parameters and {@code props},
     * the latter takes precedence.
     * <p>
     * Job parameters in the previous job execution that is to be restarted will continue
     * to be used in the restart job execution. Job parameters (as query parameters or
     * {@code java.util.Properties}) in the current invocation will complement and
     * override any same-keyed job parameters.
     *
     * @param jobExecutionId a previous job execution id
     * @param uriInfo {@code javax.ws.rs.core.UriInfo} including additional restart parameters and other info
     * @param jobParamsAsProps additional restart job parameters
     *
     * @return the new restart job execution
     *
     * @see JobResource#restart(String, UriInfo, Properties)
     * @see JobResource#start(String, UriInfo, Properties)
     */
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

    @Path("{jobExecutionId}/schedule")
    @POST
    public JobSchedule schedule(final JobScheduleConfig scheduleConfig) {
        final JobScheduler jobScheduler = JobScheduler.getJobScheduler();
        return jobScheduler.schedule(scheduleConfig);
    }

    /**
     * Gets step executions belonging to a particular job execution.
     *
     * @param jobExecutionId job execution id, for which to get step executions
     * @return step executions as {@code org.jberet.rest.entity.StepExecutionEntity[]}
     */
    @GET
    @Path("{jobExecutionId}/stepexecutions")
    public StepExecutionEntity[] getStepExecutions(final @PathParam("jobExecutionId") long jobExecutionId) {
        return JobService.getInstance().getStepExecutions(jobExecutionId);
    }

    /**
     * Gets the step execution belonging to a particular job execution and
     * having a particular step execution id.
     *
     * @param jobExecutionId job execution id
     * @param stepExecutionId step execution id
     *
     * @return a step execution of type {@code org.jberet.rest.entity.StepExecutionEntity}
     */
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

    /**
     * Sets the href field for each {@code org.jberet.rest.entity.JobExecutionEntity} passed in.
     *
     * @param uriInfo {@code javax.ws.rs.core.UriInfo}
     * @param entities 1 or more {@code org.jberet.rest.entity.JobExecutionEntity}
     */
    static void setJobExecutionEntityHref(final UriInfo uriInfo, final JobExecutionEntity... entities) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(JobExecutionResource.class);
        for (final JobExecutionEntity e : entities) {
            e.setHref(uriBuilder.clone().path(String.valueOf(e.getExecutionId())).build().toString());
        }
    }

}
