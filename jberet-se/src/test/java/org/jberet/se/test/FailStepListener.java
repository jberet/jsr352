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

import javax.batch.api.BatchProperty;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public final class FailStepListener implements javax.batch.api.listener.StepListener {
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
        System.out.println("In beforeStep method of " + this);
    }

    @Override
    public void afterStep() throws Exception {
        SleepBatchlet.appendJobExitStatus(jobContext, "afterStep");
        if (failAfterStep) {
            throw new BatchRuntimeException("failAfterStep is set to " + failAfterStep);
        }
        System.out.println("In afterStep method of " + this);
    }
}
