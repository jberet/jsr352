/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
 
package org.jberet.se.test;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.context.JobContext;
import org.jboss.logging.Logger;

@Named
@Dependent
public final class FailStepListener implements jakarta.batch.api.listener.StepListener {
    private static final Logger LOGGER = Logger.getLogger(FailStepListener.class);

    @Inject
    @BatchProperty
    private boolean failBeforeStep;

    @Inject
    @BatchProperty
    private boolean failAfterStep;

    @Inject
    private JobContext jobContext;
    
    @Override
    public void beforeStep() throws Exception {
        SleepBatchlet.appendJobExitStatus(jobContext, "beforeStep");
        if (failBeforeStep) {
            throw new BatchRuntimeException("failBeforeStep is set to " + failBeforeStep);
        }
        LOGGER.info("In beforeStep method of " + this);
    }

    @Override
    public void afterStep() throws Exception {
        SleepBatchlet.appendJobExitStatus(jobContext, "afterStep");
        if (failAfterStep) {
            throw new BatchRuntimeException("failAfterStep is set to " + failAfterStep);
        }
        LOGGER.info("In afterStep method of " + this);
    }
}
