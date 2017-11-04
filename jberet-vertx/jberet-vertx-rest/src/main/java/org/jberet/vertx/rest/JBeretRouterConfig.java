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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.vertx.core.MultiMap;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
 * servicing JBeret REST API.
 *
 * @since 1.3.0.Beta7
 */
public class JBeretRouterConfig {

    public static void config(final Router router) {
        router.route().handler(BodyHandler.create());
        router.get("/").handler(JBeretRouterConfig::getDefault);
        router.get("/jobs").handler(JBeretRouterConfig::getJobs);
        router.post("/jobs/:jobXmlName/start").handler(JBeretRouterConfig::startJob);
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
    }

    public static void getDefault(final RoutingContext routingContext) {
        routingContext.response().end("JBeret Vert.x REST API");
    }

    public static void getJobs(final RoutingContext routingContext) {
        final JobEntity[] jobEntities = JobService.getInstance().getJobs();
        final JsonArray jsonArray = new JsonArray();
        for (JobEntity jobEntity : jobEntities) {
            jsonArray.add(JsonObject.mapFrom(jobEntity));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    public static void startJob(final RoutingContext routingContext) {
        final String jobXmlName = routingContext.pathParam("jobXmlName");
        final Properties jobParams = getJobParameters(routingContext);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().start(jobXmlName, jobParams);
        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    public static void restartJob(final RoutingContext routingContext) {
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

    public static void getJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().getJobExecution(jobExecutionId);

        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    public static void getStepExecutions(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final StepExecutionEntity[] stepExecutions = JobService.getInstance().getStepExecutions(jobExecutionId);

        final JsonArray jsonArray = new JsonArray();
        for (StepExecutionEntity e : stepExecutions) {
            jsonArray.add(JsonObject.mapFrom(e));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    public static void getStepExecution(final RoutingContext routingContext) {
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

    public static void abandonJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        JobService.getInstance().abandon(jobExecutionId);
        routingContext.response().end();
    }

    public static void stopJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        JobService.getInstance().stop(jobExecutionId);
        routingContext.response().end();
    }

    public static void restartJobExecution(final RoutingContext routingContext) {
        final long jobExecutionId = getIdAsLong(routingContext, "jobExecutionId");
        final Properties jobParams = getJobParameters(routingContext);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().restart(jobExecutionId, jobParams);

        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        sendJsonResponse(routingContext, jsonObject.encodePrettily());
    }

    public static void getRunningExecutions(final RoutingContext routingContext) {
        final String jobName = routingContext.request().getParam("jobName");
        final JobExecutionEntity[] runningExecutions = JobService.getInstance().getRunningExecutions(jobName);
        final JsonArray jsonArray = new JsonArray();
        for (JobExecutionEntity e : runningExecutions) {
            jsonArray.add(JsonObject.mapFrom(e));
        }
        sendJsonResponse(routingContext, jsonArray.encodePrettily());
    }

    public static void getJobExecutions(final RoutingContext routingContext) {
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

    public static void getJobInstances(final RoutingContext routingContext) {
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

    public static void getJobInstanceCount(final RoutingContext routingContext) {
        final String jobName = routingContext.request().getParam("jobName");
        final int jobInstanceCount = JobService.getInstance().getJobInstanceCount(jobName);
        routingContext.response().end(String.valueOf(jobInstanceCount));
    }

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
}
