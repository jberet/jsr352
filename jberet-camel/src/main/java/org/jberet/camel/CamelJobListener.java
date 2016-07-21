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

package org.jberet.camel;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.listener.JobListener;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import _private.JBeretCamelLogger;
import org.apache.camel.ProducerTemplate;

/**
 * An implementation of {@code javax.batch.api.listener.JobListener} that sends
 * job execution events to a Camel endpoint. Two types of events are sent:
 * <ul>
 *     <li>{@value #HEADER_VALUE_BEFORE_JOB}: sent before a job execution
 *     <li>{@value #HEADER_VALUE_AFTER_JOB}: sent after a job execution
 * </ul>
 * The body of the message sent is the current {@code JobExecution}.
 * Each message also contains a header to indicate the event type:
 * its key is {@value #HEADER_KEY_EVENT_TYPE}, and value is either
 * {@value #HEADER_VALUE_BEFORE_JOB} or {@value #HEADER_VALUE_AFTER_JOB}.
 * <p>
 * The target Camel endpoint is configured through batch property
 * {@code endpoint} in job XML. For example,
 * <pre>
 *&lt;job id="camelJobListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0"&gt;
 *    &lt;listeners&gt;
 *       &lt;listener ref="camelJobListener"&gt;
 *           &lt;properties&gt;
 *               &lt;property name="endpoint" value="#{jobParameters['endpoint']}"/&gt;
 *           &lt;/properties&gt;
 *       &lt;/listener&gt;
 *    &lt;/listeners&gt;
 * </pre>
 *
 * @since 1.3.0
 */
@Named
public class CamelJobListener extends CamelArtifactBase implements JobListener {
    /**
     * The key of the message header to indicate whether the event is for
     * before job or after job execution.
     */
    public static final String HEADER_KEY_EVENT_TYPE = "EVENT_TYPE";

    /**
     * The value of the message header to indicate that the event is for
     * before job execution.
     */
    public static final String HEADER_VALUE_BEFORE_JOB = "BEFORE_JOB";

    /**
     * The value of the message header to indicate that the event is for
     * after job execution.
     */
    public static final String HEADER_VALUE_AFTER_JOB = "AFTER_JOB";

    /**
     * Injection of {@code javax.batch.runtime.context.JobContext} by batch
     * runtime.
     */
    @Inject
    protected JobContext jobContext;

    /**
     * Batch job operator.
     */
    protected JobOperator jobOperator;

    /**
     * Camel producer template used to send job execution events.
     */
    protected ProducerTemplate producerTemplate;

    @PostConstruct
    private void postConstruct() {
        init();
        if (producerTemplate == null) {
            producerTemplate = camelContext.createProducerTemplate();
        }
        try {
            producerTemplate.start();
        } catch (Exception e) {
            throw new BatchRuntimeException(e);
        }
        jobOperator = BatchRuntime.getJobOperator();
    }

    @PreDestroy
    private void preDestroy() {
        if (producerTemplate != null) {
            try {
                producerTemplate.stop();
            } catch (Exception e) {
                JBeretCamelLogger.LOGGER.failToStop(e, this);
            }
        }
    }

    @Override
    public void beforeJob() throws Exception {
        sendBodyAndHeader(HEADER_VALUE_BEFORE_JOB);
    }

    @Override
    public void afterJob() throws Exception {
        sendBodyAndHeader(HEADER_VALUE_AFTER_JOB);
    }

    /**
     * Sends the job execution event message to the configured Camel endpoint.
     * The message has the current {@code JobExecution} as the body, and
     * a header to indicate the event type.
     *
     * @param headerValue either {@value #HEADER_VALUE_BEFORE_JOB} or {@value #HEADER_VALUE_AFTER_JOB}
     */
    protected void sendBodyAndHeader(final String headerValue) {
        final long executionId = jobContext.getExecutionId();
        final JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        producerTemplate.sendBodyAndHeader(endpoint, jobExecution, HEADER_KEY_EVENT_TYPE, headerValue);
    }
}
