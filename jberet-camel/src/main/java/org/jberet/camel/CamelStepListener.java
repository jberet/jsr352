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

import javax.batch.api.listener.StepListener;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import static org.jberet.camel.EventType.AFTER_STEP;
import static org.jberet.camel.EventType.BEFORE_STEP;

/**
 * An implementation of {@code javax.batch.api.listener.StepListener} that sends
 * step execution events to a Camel endpoint. Two types of events are sent:
 * <ul>
 *     <li>{@value org.jberet.camel.EventType#BEFORE_STEP}: sent before a step execution
 *     <li>{@value org.jberet.camel.EventType#AFTER_STEP}: sent after a step execution
 * </ul>
 * The body of the message sent is the current {@code StepExecution}.
 * Each message also contains a header to indicate the event type:
 * its key is {@value org.jberet.camel.EventType#KEY}, and value is either
 * {@value org.jberet.camel.EventType#BEFORE_STEP} or
 * {@value org.jberet.camel.EventType#AFTER_STEP}.
 * <p>
 * The target Camel endpoint is configured through batch property
 * {@code endpoint} in job XML. For example,
 * <pre>
 * &lt;job id="camelStepListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0"&gt;
 *   &lt;step id="camelStepListenerTest.step1"&gt;
 *     &lt;listeners&gt;
 *       &lt;listener ref="camelStepListener"&gt;
 *         &lt;properties&gt;
 *           &lt;property name="endpoint" value="#{jobParameters['endpoint']}"/&gt;
 *         &lt;/properties&gt;
 *       &lt;/listener&gt;
 *     &lt;/listeners&gt;
 *     ... ...
 *   &lt;/step&gt;
 * &lt;/job&gt;
 * </pre>
 *
 * @see CamelJobListener
 * @see CamelChunkListener
 * @since 1.3.0
 */
@Named
public class CamelStepListener extends CamelListenerBase implements StepListener {
    /**
     * Injection of {@code javax.batch.runtime.context.StepContext} by batch
     * runtime.
     */
    @Inject
    protected StepContext stepContext;

    @Override
    public void beforeStep() throws Exception {
        sendBodyAndHeader(BEFORE_STEP);
    }

    @Override
    public void afterStep() throws Exception {
        sendBodyAndHeader(AFTER_STEP);
    }

    /**
     * Sends the step execution event message to the configured Camel endpoint.
     * The message has the current {@code StepExecution} as the body, and
     * a header to indicate the event type.
     *
     * @param headerValue either {@value org.jberet.camel.EventType#BEFORE_STEP}
     *                    or {@value org.jberet.camel.EventType#AFTER_STEP}
     */
    protected void sendBodyAndHeader(final String headerValue) {
        final long jobExecutionId = jobContext.getExecutionId();
        final long stepExecutionId = stepContext.getStepExecutionId();
        StepExecution stepExecution = null;
        for (final StepExecution e : jobOperator.getStepExecutions(jobExecutionId)) {
            if (stepExecutionId == e.getStepExecutionId()) {
                stepExecution = e;
                break;
            }
        }

        producerTemplate.sendBodyAndHeader(endpoint, stepExecution, EventType.KEY, headerValue);
    }
}
