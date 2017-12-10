/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.jberet.rest.entity.JobEntity;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.rest.entity.JobInstanceEntity;
import org.jberet.rest.entity.StepExecutionEntity;
import org.jberet.rest.service.JobService;

/**
 * This class is responsible for configuring the vert.x router for
 * servicing JBeret REST API. The follow shows how to configure it in your application:
 *
 * <pre>
 *   Router router = Router.router(vertx);
 *   JBeretRouterConfig.config(router);
 *   vertx.createHttpServer().requestHandler(router::accept).listen(8080);
 </pre>
 *
 * @since 1.3.0.Beta7
 */
public enum JBeretRouterConfig {
    ;

    /**
     * Default schedule delay is 5 minutes.
     */
    private static final int DEFAULT_SCHEDULE_DELAY = 5;

    /**
     * Name of the local map used to store job schedules
     */
    private static final String TIMER_LOCAL_MAP_NAME = "timer-local-map";

    /**
     * Configures REST API routes based on request URI.
     *
     * <ul>
     *     <li>
     *         Default mapping
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /
     *             <li>Query Params: None
     *             <li>Return: default return value
     *             <li>Examples: http://localhost:8080/
     *         </ul>
     *     </li>
     *     <li>
     *         Get jobs
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobs
     *             <li>Query Params: None
     *             <li>Return: JSON array of jobs
     *             <li>Examples: http://localhost:8080/jobs
     *         </ul>
     *     </li>
     *     <li>
     *         Start a job execution with a job name
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /jobs/:jobXmlName/start
     *             <li>Query Params: 0 or more job parameters
     *             <li>Return: job execution as JSON
     *             <li>Examples:
     *                  <ul>
     *                     <li>http://localhost:8080/jobs/simple/start
     *                     <li>http://localhost:8080/jobs/simple/start?sleepSeconds=4&foo=bar
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Schedule a job execution with a job name
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /jobs/:jobXmlName/schedule
     *             <li>Query Params:
     *                 <ul>
     *                     <li>delay: required, int number indicating the number of minutes to delay before starting job execution
     *                     <li>periodic: flag to control whether the schedule is recurring or not, optional and defaults to false
     *                 </ul>
     *             </li>
     *             <li>Return: job schedule as JSON
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobs/simple/schedule?delay=1
     *                     <li>http://localhost:8080/jobs/simple/schedule?delay=1&periodic=true
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         restart the most recently failed or stopped job execution belonging to the job name
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /jobs/:jobXmlName/restart
     *             <li>Query Params: 0 or more job parameters to override the corresponding original job parameters
     *             <li>Return: job execution as JSON
     *             <li>Examples:
     *                  <ul>
     *                     <li>http://localhost:8080/jobs/simple/restart
     *                     <li>http://localhost:8080/jobs/simple/restart?sleepSeconds=5&foo=bar
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get job instances
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobinstances
     *             <li>Query Params:
     *                 <ul>
     *                     <li>jobName: the job name used to get the associated job instances, required unless jobExecutionId is present
     *                     <li>start: the offset position in the list of all eligible job instances to include, optional and defaults to 0
     *                     <li>count: the number of job instances to return, optional and defaults to all job instances
     *                     <li>jobExecutionId: the job execution id used to get the associated job instance.
     *                         This param should not be used along with jobName, start, or count.
     *                 </ul>
     *             </li>
     *             <li>JSON array of job instances
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobinstances?jobName=simple&count=2
     *                     <li>http://localhost:8080/jobinstances?jobExecutionId=1
     *                     <li>http://localhost:8080/jobinstances?jobName=simple&count=2&start=1
     *                     <li>http://localhost:8080/jobinstances/count?jobName=simple
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Count the number of job instances
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobinstances/count
     *             <li>Query Params:
     *                 <ul>
     *                     <li>jobName: the job name used to get the associated job instances, required.
     *                 </ul>
     *             </li>
     *             <li>Return: number of job instances
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobinstances/count?jobName=simple
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get job executions
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobexecutions
     *             <li>Query Params:
     *                 <ul>
     *                     <li>count: the number of job executions to return, optional and defaults to all matching job executions
     *                     <li>jobExecutionId1: the job execution id whose sibling job executions will be returned, optional and defaults to unspecified
     *                 </ul>
     *             </li>
     *             <li>Return: JSON array of job executions
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions
     *                     <li>http://localhost:8080/jobexecutions?count=5
     *                     <li>http://localhost:8080/jobexecutions?jobExecutionId1=2
     *                     <li>http://localhost:8080/jobexecutions?jobExecutionId1=1&count=10
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get running job executions associated with a job name
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobexecutions/running
     *             <li>Query Params:
     *                 <ul>
     *                     <li>jobName: the job name used to get the associated job instances, required.
     *                 </ul>
     *             </li>
     *             <li>Return: JSON array of job executions
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/running?jobName=simple'
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get job execution by id
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobexecutions/:jobExecutionId
     *             <li>Query Params: None
     *             <li>Return: job execution as JSON
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/1
     *                     <li>http://localhost:8080/jobexecutions/2
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get step executions belonging to a particular job execution
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobexecutions/:jobExecutionId/stepexecutions
     *             <li>Query Params: None
     *             <li>Return: JSON array of step executions
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/1/stepexecutions
     *                     <li>http://localhost:8080/jobexecutions/15/stepexecutions
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get the step execution by job execution id and step execution id
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /jobexecutions/:jobExecutionId/stepexecutions/:stepExecutionId
     *             <li>Query Params: None
     *             <li>Return: step execution as JSON
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/1/stepexecutions/1
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Abandon a job execution
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /jobexecutions/:jobExecutionId/abandon
     *             <li>Query Params: None
     *             <li>Return: void
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/1/abandon
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Stop a job execution
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /jobexecutions/:jobExecutionId/stop
     *             <li>Query Params: None
     *             <li>Return: void
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/2/stop
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Restart a failed or stopped job execution
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /jobexecutions/:jobExecutionId/restart
     *             <li>Query Params: 0 or more job parameters to override the corresponding original job parameters
     *             <li>Return: job execution as JSON
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/jobexecutions/1/restart
     *                     <li>http://localhost:8080/jobexecutions/1/restart?sleepSeconds=3&foo=buzz
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get all job schedules
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /schedules
     *             <li>Query Params: None
     *             <li>Return: JSON array of job schedules
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/schedules
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get job schedule by id
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /schedules/:scheduleId
     *             <li>Query Params: None
     *             <li>Return: job schedule as JSON
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/schedules/2
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get all timezone values available to job schedules
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /schedules/timezones
     *             <li>Query Params: None
     *             <li>Return: JSON array of all timezone values
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/schedules/timezones
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Get supported extra features as a string array, currently return []
     *         <ul>
     *             <li>HTTP Method: GET
     *             <li>URI Pattern: /schedules/features
     *             <li>Query Params: None
     *             <li>Return: JSON array, currently return empty array
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/schedules/features
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Cancel a job schedule
     *         <ul>
     *             <li>HTTP Method: POST
     *             <li>URI Pattern: /schedules/:scheduleId/cancel
     *             <li>Query Params: None
     *             <li>Return: true if the job schedule is cancelled successfully; false otherwise
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/schedules/0/cancel
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Delete a job schedule
     *         <ul>
     *             <li>HTTP Method: DELETE
     *             <li>URI Pattern: /schedules/:scheduleId
     *             <li>Query Params: None
     *             <li>Return: void
     *             <li>Examples:
     *                 <ul>
     *                     <li>http://localhost:8080/schedules/0
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param router the vert.x router for the current request
     */
    public static void config(final Router router) {
        router.route().handler(BodyHandler.create());
        router.get("/").handler(JBeretRouterConfig::getDefault);
        router.get("/jobs").handler(JBeretRouterConfig::getJobs);

        router.post("/jobs/:jobXmlName/start").handler(JBeretRouterConfig::startJob);
        router.post("/jobs/:jobXmlName/schedule").handler(JBeretRouterConfig::scheduleJob);
        router.post("/jobs/:jobXmlName/restart").handler(JBeretRouterConfig::restartJob);

        router.get("/jobinstances").handler(JBeretRouterConfig::getJobInstances);
        router.get("/jobinstances/count").handler(JBeretRouterConfig::getJobInstanceCount);

        router.get("/jobexecutions").handler(JBeretRouterConfig::getJobExecutions);
        router.get("/jobexecutions/running").handler(JBeretRouterConfig::getRunningExecutions);
        router.get("/jobexecutions/:jobExecutionId").handler(JBeretRouterConfig::getJobExecution);
        router.get("/jobexecutions/:jobExecutionId/stepexecutions").handler(JBeretRouterConfig::getStepExecutions);
        router.get("/jobexecutions/:jobExecutionId/stepexecutions/:stepExecutionId").handler(JBeretRouterConfig::getStepExecution);

        router.post("/jobexecutions/:jobExecutionId/abandon").handler(JBeretRouterConfig::abandonJobExecution);
        router.post("/jobexecutions/:jobExecutionId/stop").handler(JBeretRouterConfig::stopJobExecution);
        router.post("/jobexecutions/:jobExecutionId/restart").handler(JBeretRouterConfig::restartJobExecution);

        router.get("/schedules").handler(JBeretRouterConfig::getJobSchedules);
        router.get("/schedules/timezones").handler(JBeretRouterConfig::getJobSchedulesTimezone);
        router.get("/schedules/features").handler(JBeretRouterConfig::getJobSchedulesFeatures);
        router.get("/schedules/:scheduleId").handler(JBeretRouterConfig::getJobSchedule);
        router.post("/schedules/:scheduleId/cancel").handler(JBeretRouterConfig::cancelJobSchedule);
        router.delete("/schedules/:scheduleId").handler(JBeretRouterConfig::deleteJobSchedule);
    }

    private static void getDefault(final RoutingContext routingContext) {
        routingContext.response().end("JBeret Vert.x REST API");
    }

    private static void getJobs(final RoutingContext routingContext) {
        final JobEntity[] jobEntities = JobService.getInstance().getJobs();
        final JsonArray jsonArray = new JsonArray();
        for (JobEntity jobEntity : jobEntities) {
            jsonArray.add(JsonObject.mapFrom(jobEntity));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    private static void startJob(final RoutingContext routingContext) {
        final String jobXmlName = routingContext.pathParam("jobXmlName");
        final Properties jobParams = getJobParameters(routingContext);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().start(jobXmlName, jobParams);
        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    private static void scheduleJob(final RoutingContext routingContext) {
        final String jobXmlName = routingContext.pathParam("jobXmlName");
        final HttpServerRequest request = routingContext.request();
        final String delayString = request.getParam("delay");
        final long delay = delayString == null ? DEFAULT_SCHEDULE_DELAY : Long.parseLong(delayString);
        final String periodicString = request.getParam("periodic");
        final boolean periodic = periodicString != null && Boolean.parseBoolean(periodicString);

        final Properties jobParams = getJobParameters(routingContext);
        final JobSchedule jobSchedule = new JobSchedule();
        jobSchedule.setDelay(delay);
        jobSchedule.setJobName(jobXmlName);
        jobSchedule.setJobParameters(jobParams);

        final long delayMillis = delay * 60 * 1000;
        final long timerId;
        final Vertx vertx = routingContext.vertx();
        if (!periodic) {
            timerId = vertx.setTimer(delayMillis, timerId1 -> {
                final JobExecutionEntity jobExecutionEntity = JobService.getInstance().start(jobXmlName, jobParams);
                jobSchedule.addJobExecutionIds(jobExecutionEntity.getExecutionId());
                jobSchedule.setStatus(JobSchedule.Status.DONE);
            });
        } else {
            timerId = vertx.setPeriodic(delayMillis, timerId1 -> {
                final JobExecutionEntity jobExecutionEntity = JobService.getInstance().start(jobXmlName, jobParams);
                jobSchedule.addJobExecutionIds(jobExecutionEntity.getExecutionId());

//                 since this is periodic timer, there may be more in the future
//                jobSchedule.setStatus(JobSchedule.Status.DONE);
            });
        }
        jobSchedule.setId(timerId);
        final LocalMap<String, JobSchedule> timerLocalMap = getTimerLocalMap(vertx);
        timerLocalMap.put(String.valueOf(timerId), jobSchedule);
        final JsonObject jsonObject = JsonObject.mapFrom(jobSchedule);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    private static void restartJob(final RoutingContext routingContext) {
        final String jobXmlName = routingContext.pathParam("jobXmlName");
        final Properties jobParams = getJobParameters(routingContext);

        final JobInstanceEntity[] jobInstances = JobService.getInstance().getJobInstances(jobXmlName, 0, 1);
        if (jobInstances.length > 0) {
            final long latestJobExecutionId = jobInstances[0].getLatestJobExecutionId();
            final JobExecutionEntity jobExecutionEntity = JobService.getInstance().restart(latestJobExecutionId, jobParams);

            final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
            sendJsonResponse(routingContext, jsonObject.encodePrettily());
        } else {
            throw new VertxException(routingContext.normalisedPath());
        }
    }

    private static void getJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().getJobExecution(jobExecutionId);

        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    private static void getStepExecutions(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final StepExecutionEntity[] stepExecutions = JobService.getInstance().getStepExecutions(jobExecutionId);

        final JsonArray jsonArray = new JsonArray();
        for (StepExecutionEntity e : stepExecutions) {
            jsonArray.add(JsonObject.mapFrom(e));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    private static void getStepExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final long stepExecutionId = getIdAsLong(routingContext, "stepExecutionId");
        StepExecutionEntity stepExecutionFound = null;
        final StepExecutionEntity[] stepExecutions = JobService.getInstance().getStepExecutions(jobExecutionId);
        for (StepExecutionEntity e : stepExecutions) {
            if (e.getStepExecutionId() == stepExecutionId) {
                stepExecutionFound = e;
            }
        }

        final JsonObject jsonObject = JsonObject.mapFrom(stepExecutionFound);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    private static void abandonJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        JobService.getInstance().abandon(jobExecutionId);
        routingContext.response().end();
    }

    private static void stopJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        JobService.getInstance().stop(jobExecutionId);
        routingContext.response().end();
    }

    private static void restartJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final Properties jobParams = getJobParameters(routingContext);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().restart(jobExecutionId, jobParams);

        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    private static void getRunningExecutions(final RoutingContext routingContext) {
        final String jobName = routingContext.request().getParam("jobName");
        final JobExecutionEntity[] runningExecutions = JobService.getInstance().getRunningExecutions(jobName);
        final JsonArray jsonArray = new JsonArray();
        for (JobExecutionEntity e : runningExecutions) {
            jsonArray.add(JsonObject.mapFrom(e));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    private static void getJobExecutions(final RoutingContext routingContext) {
        final String jobExecutionId1String = routingContext.request().getParam("jobExecutionId1");
        final long jobExecutionId1 = jobExecutionId1String == null ? 0 : Long.parseLong(jobExecutionId1String);
        final String countString = routingContext.request().getParam("count");
        final int count = countString == null ? 0 : Integer.parseInt(countString);

        //jobExecutionId1 is used to retrieve the JobInstance, from which to get all its JobExecution's
        //jobInstanceId param is currently not used.
        final JobExecutionEntity[] jobExecutionEntities =
                JobService.getInstance().getJobExecutions(count, 0, jobExecutionId1);
        final JsonArray jsonArray = new JsonArray();
        for (JobExecutionEntity e : jobExecutionEntities) {
            jsonArray.add(JsonObject.mapFrom(e));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    private static void getJobInstances(final RoutingContext routingContext) {
        final HttpServerRequest request = routingContext.request();

        final String jobName = request.getParam("jobName");

        final String startString = request.getParam("start");
        final int start = startString == null ? 0 : Integer.parseInt(startString);

        final String countString = request.getParam("count");
        final int count = countString == null ? 0 : Integer.parseInt(countString);

        final String jobExecutionIdString = request.getParam("jobExecutionId");
        final long jobExecutionId = jobExecutionIdString == null ? 0 : Long.parseLong(jobExecutionIdString);

        if (jobExecutionId > 0) {
            final JobInstanceEntity jobInstanceData = JobService.getInstance().getJobInstance(jobExecutionId);
            final JsonObject jsonObject = JsonObject.mapFrom(jobInstanceData);
            sendJsonResponse(routingContext, jsonObject.encodePrettily());
        } else {
            final JobInstanceEntity[] jobInstanceData =
                    JobService.getInstance().getJobInstances(jobName == null ? "*" : jobName, start,
                            count == 0 ? Integer.MAX_VALUE : count);
            final JsonArray jsonArray = new JsonArray();
            for (JobInstanceEntity e : jobInstanceData) {
                jsonArray.add(JsonObject.mapFrom(e));
            }
            sendJsonResponse(routingContext, jsonArray.encodePrettily());
        }
    }

    private static void getJobInstanceCount(final RoutingContext routingContext) {
        final String jobName = routingContext.request().getParam("jobName");
        final int jobInstanceCount = JobService.getInstance().getJobInstanceCount(jobName);
        routingContext.response().end(String.valueOf(jobInstanceCount));
    }

    private static void getJobSchedules(final RoutingContext routingContext) {
        final LocalMap<String, JobSchedule> timerLocalMap = getTimerLocalMap(routingContext.vertx());
        final JsonArray jsonArray = new JsonArray();
        for (JobSchedule jobSchedule : timerLocalMap.values()) {
            jsonArray.add(JsonObject.mapFrom(jobSchedule));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    private static void getJobSchedule(final RoutingContext routingContext) {
        final JobSchedule jobSchedule = lookupJobScheduleWithPathParam(routingContext);
        final JsonObject jsonObject = JsonObject.mapFrom(jobSchedule);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    private static void cancelJobSchedule(final RoutingContext routingContext) {
        final JobSchedule jobSchedule = lookupJobScheduleWithPathParam(routingContext);
        boolean cancelled = false;

        if (jobSchedule != null) {
            cancelled = routingContext.vertx().cancelTimer(jobSchedule.getId());
            if (cancelled) {
                jobSchedule.setStatus(JobSchedule.Status.CANCELLED);
            }
        }
        routingContext.response().end(String.valueOf(cancelled));
    }

    private static void getJobSchedulesFeatures(final RoutingContext routingContext) {
        routingContext.response().end(new JsonArray().encode());
    }

    private static void getJobSchedulesTimezone(final RoutingContext routingContext) {
        final String[] availableIDs = TimeZone.getAvailableIDs();
        Arrays.sort(availableIDs);
        final int i = Arrays.binarySearch(availableIDs, TimeZone.getDefault().getID());
        final String[] result = new String[availableIDs.length];
        result[0] = availableIDs[i];
        System.arraycopy(availableIDs, 0, result, 1, i);
        System.arraycopy(availableIDs, i + 1, result, i + 1, availableIDs.length - (i + 1));

        final JsonArray jsonArray = new JsonArray();
        Arrays.stream(result).forEach(jsonArray::add);
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    private static void deleteJobSchedule(final RoutingContext routingContext) {
        final String idString = routingContext.pathParam("scheduleId");
        final LocalMap<String, JobSchedule> timerLocalMap = getTimerLocalMap(routingContext.vertx());
        final JobSchedule removedItem = timerLocalMap.remove(idString);

        if (removedItem != null) {
            routingContext.vertx().cancelTimer(removedItem.getId());
        }
        routingContext.response().end();
    }


    // Helper methods

    private static Properties getJobParameters(final RoutingContext routingContext) {
        final MultiMap params = routingContext.request().params();
        final List<Map.Entry<String, String>> entries = params.entries();
        final Properties jobParams = new Properties();
        for (Map.Entry<String, String> entry : entries) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            jobParams.setProperty(key, value);
        }
        return jobParams;
    }

    private static long getIdAsLong(final RoutingContext routingContext, final String pathParamKey) {
        final String idString = routingContext.pathParam(pathParamKey);
        return Long.parseLong(idString);
    }

    private static void sendJsonResponse(final RoutingContext routingContext, String body) {
        routingContext.response().putHeader("content-type", "application/json").end(body);
    }

    private static LocalMap<String, JobSchedule> getTimerLocalMap(Vertx vertx) {
        return vertx.sharedData().getLocalMap(TIMER_LOCAL_MAP_NAME);
    }

    private static JobSchedule lookupJobScheduleWithPathParam(final RoutingContext routingContext) {
        final LocalMap<String, JobSchedule> timerLocalMap = getTimerLocalMap(routingContext.vertx());
        final String idString = routingContext.pathParam("scheduleId");
        return timerLocalMap.get(idString);
    }

}
