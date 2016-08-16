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

package org.jberet.camel.component;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.JobExecution;

import _private.JBeretCamelMessages;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;

/**
 * Camel producer class defining JBeret producer. This producer supports the
 * following URI structure (examples are given):
 * <ul>
 *   <li>{@code jberet:jobs}<p>
 *       lists job names. The response type is {@code java.util.Set<String>}.
 *   <li>{@code jberet:jobs/job1}<p>
 *       starts the job {@code job1}. any job parameters
 *       should be passed as message body as {@code java.util.Properties}.
 *       The response type is {@code long} (job execution id).
 *   <li>{@code jberet:jobs/job1/start}<p>
 *       starts the job {@code job1}, same as above. any job parameters
 *       should be passed as message body as {@code java.util.Properties}.
 *       The response type is {@code long} (job execution id).
 *   <li>{@code jberet:jobinstances?jobName=job1&start=0&count=10}<p>
 *       lists job instances, {@code jobName} query param is required, {@code start}
 *       and {@code count} query params are both optional, and defaults to 0 and 10,
 *       respectively. The response type is {@code java.util.List<JobInstance>}.
 *   <li>{@code jberet:jobinstances/count?jobName=job1}<p>
 *       counts job instances of the job {@code job1}. {@code jobName} query param is
 *       required. The response type is {@code int}.
 *   <li>{@code jberet:jobexecutions/running?jobName=job1}<p>
 *       lists all running job executions of the job {@code job1}. {@code jobName} query param
 *       is usually specified, but if omitted, it defaults to {@code *} and lists running
 *       job executions of all jobs currently known to the batch runtime.
 *       The response type is {@code java.util.List<Long>} (job execution ids).
 *   <li>{@code jberet:jobexecutions/123456}<p>
 *       gets the job execution with id {@code 123456}.
 *       The response type is {@code JobExecution}.
 *   <li>{@code jberet:jobexecutions/123456/stop}<p>
 *       stops the job execution with id {@code 123456}. No response is generated.
 *   <li>{@code jberet:jobexecutions/123456/restart}<p>
 *       restarts the job execution with id {@code 123456}. any job parameters
 *       should be passed as message body as {@code java.util.Properties}.
 *       The response type is {@code long} (job execution id).
 *   <li>{@code jberet:jobexecutions/123456/abandon}<p>
 *       abandons the job execution with id {@code 123456}. No response is generated.
 * </ul>
 *
 * @see JBeretComponent
 * @see JBeretEndpoint
 * @since 1.3.0
 */
public class JBeretProducer extends DefaultProducer {
    /**
     * String constant for "jobs", which typically appear in JBeret endpoint URI
     * to denote job-related operations.
     */
    public static final String JOBS = "jobs";

    /**
     * String constant for "jobinstances", which typically appear in JBeret endpoint URI
     * to denote jobinstance-related operations.
     */
    public static final String JOBINSTANCES = "jobinstances";

    /**
     * String constant for "jobexecutions", which typically appear in JBeret endpoint URI
     * to denote jobexecution-related operations.
     */
    public static final String JOBEXECUTIONS = "jobexecutions";

    /**
     * String constant for "start", which typically appear in JBeret endpoint URI
     * to denote starting a job, or the starting position inside a collection.
     */
    public static final String START = "start";

    /**
     * String constant for "restart", which usually appear in JBeret endpoint URI
     * to denote restarting a job execution.
     */
    public static final String RESTART = "restart";

    /**
     * String constant for "stop", which usually appear in JBeret endpoint URI
     * to denote stopping a running job execution.
     */
    public static final String STOP = "stop";

    /**
     * String constant for "abandon", which usually appear in JBeret endpoint URI
     * to denote abandoning a job execution.
     */
    public static final String ABANDON = "abandon";

    /**
     * String constant for "count", which usually appear in JBeret endpoint URI
     * to denote the query parameter {@code count}.
     */
    public static final String COUNT = "count";

    /**
     * String constant for "jobName", which usually appear in JBeret endpoint URI
     * to denote the query parameter {@code jobName}.
     */
    public static final String JOB_NAME = "jobName";

    /**
     * String constant for "running", which usually appear in JBeret endpoint URI
     * to denote running job executions.
     */
    public static final String RUNNING = "running";

    /**
     * Instantiates {@code JBeretProducer}.
     *
     * @param endpoint JBeret endpoint
     */
    public JBeretProducer(final Endpoint endpoint) {
        super(endpoint);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method invokes the appropriate batch job operations as specified in
     * the message.
     *
     * @param exchange the current Camel exchange
     * @throws Exception if any errors occur
     */
    @Override
    public void process(final Exchange exchange) throws Exception {
        final JBeretEndpoint endpoint = (JBeretEndpoint) getEndpoint();
        final JBeretComponent component = (JBeretComponent) endpoint.getComponent();
        runJobOperation(component.getJobOperator(), exchange, endpoint.getRemainingPath());
    }

    /**
     * Runs various {@code JobOperator} operations based on JBeret endpoint URI.
     *
     * @param jobOperator batch job operator
     * @param exchange Camel exchange
     * @param remainingPath the remaing path of JBeret endpoint URI (without scheme part)
     *
     * @throws Exception if any errors occur
     */
    private void runJobOperation(final JobOperator jobOperator,
                                 final Exchange exchange,
                                 final String remainingPath) throws Exception {
        if (remainingPath.isEmpty()) {
            throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(remainingPath);
        }

        final String[] parts = remainingPath.split("/");

        if (parts.length == 0) {
            throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(remainingPath);
        } else {
            final String resourceType = parts[0].toLowerCase();
            if (resourceType.equals(JOBS)) {
                doJobs(jobOperator, exchange, parts);
            } else if (JOBEXECUTIONS.equals(resourceType)) {
                doJobExecutions(jobOperator, exchange, parts);
            } else if (JOBINSTANCES.equals(resourceType)) {
                doJobInstances(jobOperator, exchange, parts);
            } else {
                throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(remainingPath);
            }
        }
    }

    /**
     * Performs operations related to "jobs", if "jobs" is the first segment of the URI.
     *
     * @param jobOperator batch job operator
     * @param exchange Camel exchange
     * @param paths string array containing segments of the URI path (without scheme part)
     *
     * @throws Exception if any errors occur
     */
    private void doJobs(final JobOperator jobOperator,
                        final Exchange exchange,
                        final String[] paths) throws Exception {
        final Message in = exchange.getIn();
        if (paths.length == 1) {
            //urls like jberet:jobs
            in.setBody(jobOperator.getJobNames(), Set.class);
        } else if (paths.length == 2) {
            //urls like jberet:jobs/job1
            in.setBody(jobOperator.start(paths[1], (Properties) in.getBody()), long.class);
        } else if (paths.length == 3) {
            //urls like jberet:jobs/job1/start
            if (START.equals(paths[2])) {
                in.setBody(jobOperator.start(paths[1], (Properties) in.getBody()), long.class);
            } else {
                throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(Arrays.toString(paths));
            }
        } else {
            throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(Arrays.toString(paths));
        }
    }

    /**
     * Performs operations related to "jobinstances", if "jobinstances" is the first segment of the URI.
     *
     * @param jobOperator batch job operator
     * @param exchange Camel exchange
     * @param paths string array containing segments of the URI path (without scheme part)
     * @throws Exception if any errors occur
     */
    private void doJobInstances(final JobOperator jobOperator,
                                final Exchange exchange,
                                final String[] paths) throws Exception {
        final Message in = exchange.getIn();
        if (paths.length == 1) {
            //urls like jberet:jobinstances?jobName=job1&start=0&count=10
            final String jobName = ((JBeretEndpoint) getEndpoint()).getJobName();
            if (jobName == null) {
                throw JBeretCamelMessages.MESSAGES.invalidOrMissingParameterInJBeretComponentUrk(JOB_NAME, null);
            }

            final int startInt = ((JBeretEndpoint) getEndpoint()).getStart();
            final int countInt = ((JBeretEndpoint) getEndpoint()).getCount();
            in.setBody(jobOperator.getJobInstances(jobName, startInt, countInt), List.class);
        } else if (paths.length == 2) {
            //urls like jberet:jobinstances/count?jobName=job1
            final String resourceName = paths[1];
            if (COUNT.equals(resourceName)) {
                final String jobName = ((JBeretEndpoint) getEndpoint()).getJobName();
                if (jobName == null) {
                    throw JBeretCamelMessages.MESSAGES.invalidOrMissingParameterInJBeretComponentUrk(JOB_NAME, null);
                }

                in.setBody(jobOperator.getJobInstanceCount(jobName), int.class);
            } else {
                //urls like jberet:jobinstances/123456,
                //wait for the spec to support getJobInstanceById(instanceId)
                throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(Arrays.toString(paths));
            }
        } else {
            throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(Arrays.toString(paths));
        }
    }

    /**
     * Performs operations related to "jobexecutions", if "jobexecutions" is the first segment of the URI.
     *
     * @param jobOperator batch job operator
     * @param exchange Camel exchange
     * @param paths string array containing segments of the URI path (without scheme part)
     * @throws Exception if any errors occur
     */
    private void doJobExecutions(final JobOperator jobOperator,
                                 final Exchange exchange,
                                 final String[] paths) throws Exception {
        final Message in = exchange.getIn();
        if (paths.length == 1) {
            //urls like jberet:jobexecutions
            throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(Arrays.toString(paths));
        } else if (paths.length == 2) {
            if (RUNNING.equals(paths[1])) {
                //urls like jberet:jobexecutions/running?jobName=job1
                String jobName = ((JBeretEndpoint) getEndpoint()).getJobName();
                if (jobName == null) {
                    jobName = "*";
                }
                in.setBody(jobOperator.getRunningExecutions(jobName), List.class);
            } else {
                //urls like jberet:jobexecutions/123456
                long jobExecutionId = Long.parseLong(paths[1]);
                in.setBody(jobOperator.getJobExecution(jobExecutionId), JobExecution.class);
            }
        } else if (paths.length == 3) {
            long jobExecutionId = Long.parseLong(paths[1]);
            final String resourceOperation = paths[2];
            if (STOP.equals(resourceOperation)) {
                //urls like jberet:jobexecutions/123456/stop
                jobOperator.stop(jobExecutionId);
            } else if (RESTART.equals(resourceOperation)) {
                //urls like jberet:jobexecutions/123456/restart
                in.setBody(jobOperator.restart(jobExecutionId, (Properties) in.getBody()), long.class);
            } else if (ABANDON.equals(resourceOperation)) {
                //urls like jberet:jobexecutions/123456/abandon
                jobOperator.abandon(jobExecutionId);
            } else {
                throw JBeretCamelMessages.MESSAGES.invalidJBeretComponentUri(Arrays.toString(paths));
            }
        }
    }
}
