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
import javax.batch.api.listener.JobListener;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public final class FailJobListener implements JobListener {
    @Inject
    @BatchProperty
    private boolean failBeforeJob;

    @Inject
    @BatchProperty
    private boolean failAfterJob;

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        SleepBatchlet.appendJobExitStatus(jobContext, "beforeJob");
        if (failBeforeJob) {
            throw new BatchRuntimeException("failBeforeJob is set to " + failBeforeJob);
        }
        System.out.printf("In beforeJob method of %s%n", this);
    }

    @Override
    public void afterJob() throws Exception {
        SleepBatchlet.appendJobExitStatus(jobContext, "afterJob");
        if (failAfterJob) {
            throw new BatchRuntimeException("failAfterJob is set to " + failAfterJob);
        }
        System.out.printf("In afterJob method of %s%n", this);
    }
}
