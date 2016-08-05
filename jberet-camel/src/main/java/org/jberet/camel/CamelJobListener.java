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

import javax.batch.api.listener.JobListener;
import javax.batch.runtime.JobExecution;
import javax.inject.Named;

/**
 * An implementation of {@code javax.batch.api.listener.JobListener} that sends
 * job execution events to a Camel endpoint. Two types of events are sent:
 * <ul>
 * <li>{@value org.jberet.camel.EventType#BEFORE_JOB}: sent before a job execution
 * <li>{@value org.jberet.camel.EventType#AFTER_JOB}: sent after a job execution
 * </ul>
 * The body of the message sent is the current {@code JobExecution}.
 * Each message also contains a header to indicate the event type:
 * its key is {@value org.jberet.camel.EventType#KEY}, and value is either
 * {@value org.jberet.camel.EventType#BEFORE_JOB} or
 * {@value org.jberet.camel.EventType#AFTER_JOB}.
 * <p>
 * The target Camel endpoint is configured through batch property
 * {@code endpoint} in job XML. For example,
 * <pre>
 * &lt;job id="camelJobListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0"&gt;
 *    &lt;listeners&gt;
 *       &lt;listener ref="camelJobListener"&gt;
 *           &lt;properties&gt;
 *               &lt;property name="endpoint" value="#{jobParameters['endpoint']}"/&gt;
 *           &lt;/properties&gt;
 *       &lt;/listener&gt;
 *    &lt;/listeners&gt;
 * </pre>
 *
 * @see CamelStepListener
 * @see CamelChunkListener
 * @since 1.3.0
 */
@Named
public class CamelJobListener extends CamelListenerBase implements JobListener {

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#BEFORE_JOB}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#BEFORE_JOB}
     * </pre>
     * , and the message body is the current {@code javax.batch.runtime.JobExecution}.
     *
     * @throws Exception
     */
    @Override
    public void beforeJob() throws Exception {
        sendBodyAndHeader(EventType.BEFORE_JOB);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#AFTER_JOB}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#AFTER_JOB}
     * </pre>
     * , and the message body is the current {@code javax.batch.runtime.JobExecution}.
     *
     * @throws Exception
     */
    @Override
    public void afterJob() throws Exception {
        sendBodyAndHeader(EventType.AFTER_JOB);
    }

    /**
     * Sends the job execution event message to the configured Camel endpoint.
     * The message has the current {@code JobExecution} as the body, and
     * a header to indicate the event type.
     *
     * @param headerValue either {@value org.jberet.camel.EventType#BEFORE_JOB}
     *                    or {@value org.jberet.camel.EventType#AFTER_JOB}
     */
    protected void sendBodyAndHeader(final String headerValue) {
        final long executionId = jobContext.getExecutionId();
        final JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        producerTemplate.sendBodyAndHeader(endpoint, jobExecution, EventType.KEY, headerValue);
    }
}
