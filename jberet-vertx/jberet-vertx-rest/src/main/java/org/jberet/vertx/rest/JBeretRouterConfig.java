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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.jberet.rest.entity.JobEntity;
import org.jberet.rest.entity.JobExecutionEntity;
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

        routingContext.response().putHeader("content-type", "application/json")
                .end(jsonArray.encodePrettily());
    }

    public static void startJob(final RoutingContext routingContext) {
        final String jobXmlName = routingContext.pathParam("jobXmlName");
        final Properties jobParams = getJobParameters(routingContext);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().start(jobXmlName, jobParams);
        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        routingContext.response().putHeader("content-type", "application/json")
                .end(jsonObject.encodePrettily());

    }

    public static void getJobExecution(final RoutingContext routingContext) {
        final String jobExecutionIdString = routingContext.pathParam("jobExecutionId");
        final long jobExecutionId = Long.parseLong(jobExecutionIdString);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().getJobExecution(jobExecutionId);

        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        routingContext.response().putHeader("content-type", "application/json")
                .end(jsonObject.encodePrettily());
    }

    public static void getStepExecutions(final RoutingContext routingContext) {
        final String jobExecutionIdString = routingContext.pathParam("jobExecutionId");
        final long jobExecutionId = Long.parseLong(jobExecutionIdString);
        final StepExecutionEntity[] stepExecutions = JobService.getInstance().getStepExecutions(jobExecutionId);

        final JsonArray jsonArray = new JsonArray();
        for (StepExecutionEntity e : stepExecutions) {
            jsonArray.add(JsonObject.mapFrom(e));
        }

        routingContext.response().putHeader("content-type", "application/json")
                .end(jsonArray.encodePrettily());
    }

    public static void getStepExecution(final RoutingContext routingContext) {
        final String jobExecutionIdString = routingContext.pathParam("jobExecutionId");
        final long jobExecutionId = Long.parseLong(jobExecutionIdString);
        final String stepExecutionIdString = routingContext.pathParam("stepExecutionId");
        final long stepExecutionId = Long.parseLong(stepExecutionIdString);

        StepExecutionEntity stepExecutionFound = null;
        final StepExecutionEntity[] stepExecutions = JobService.getInstance().getStepExecutions(jobExecutionId);
        for (StepExecutionEntity e : stepExecutions) {
            if (e.getStepExecutionId() == stepExecutionId) {
                stepExecutionFound = e;
            }
        }

        final JsonObject jsonObject = JsonObject.mapFrom(stepExecutionFound);
        routingContext.response().putHeader("content-type", "application/json")
                .end(jsonObject.encodePrettily());
    }

    public static void abandonJobExecution(final RoutingContext routingContext) {
        final String jobExecutionIdString = routingContext.pathParam("jobExecutionId");
        final long jobExecutionId = Long.parseLong(jobExecutionIdString);
        JobService.getInstance().abandon(jobExecutionId);
        routingContext.response().end();
    }

    public static void stopJobExecution(final RoutingContext routingContext) {
        final String jobExecutionIdString = routingContext.pathParam("jobExecutionId");
        final long jobExecutionId = Long.parseLong(jobExecutionIdString);
        JobService.getInstance().stop(jobExecutionId);
        routingContext.response().end();
    }

    public static void restartJobExecution(final RoutingContext routingContext) {
        final String jobExecutionIdString = routingContext.pathParam("jobExecutionId");
        final long jobExecutionId = Long.parseLong(jobExecutionIdString);
        final Properties jobParams = getJobParameters(routingContext);
        final JobExecutionEntity jobExecutionEntity = JobService.getInstance().restart(jobExecutionId, jobParams);

        final JsonObject jsonObject = JsonObject.mapFrom(jobExecutionEntity);
        routingContext.response().putHeader("content-type", "application/json")
                .end(jsonObject.encodePrettily());
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

}
