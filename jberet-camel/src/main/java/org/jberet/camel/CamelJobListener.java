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

@Named
public class CamelJobListener extends CamelArtifactBase implements JobListener {
    public static final String HEADER_KEY_EVENT_TYPE = "EVENT_TYPE";
    public static final String HEADER_VALUE_BEFORE_JOB = "BEFORE_JOB";
    public static final String HEADER_VALUE_AFTER_JOB = "AFTER_JOB";

    @Inject
    protected JobContext jobContext;

    protected JobOperator jobOperator;

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

    protected void sendBodyAndHeader(final String headerValue) {
        final long executionId = jobContext.getExecutionId();
        final JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        producerTemplate.sendBodyAndHeader(endpoint, jobExecution, HEADER_KEY_EVENT_TYPE, headerValue);
    }
}
