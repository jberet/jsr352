/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@Dependent
public class SleepBatchlet extends AbstractBatchlet implements Batchlet {
    static final String SLEPT = "Slept";

    @Inject
    @BatchProperty(name = "sleep.minutes")
    private int sleepMinutes;

    @Inject
    @BatchProperty
    private boolean failInProcess;

    private Thread processThread;

    @Override
    public String process() throws Exception {
        processThread = Thread.currentThread();
        if (failInProcess) {
            throw new BatchRuntimeException("failInProcess is set to true");
        }

        try {
            Thread.sleep(sleepMinutes * 60 * 1000);
        } catch (final InterruptedException e) {
            return BatchStatus.STOPPED.name();
        }
        return SLEPT;
    }

    @Override
    public void stop() throws Exception {
        System.out.printf("in @Stop, %s%n", Thread.currentThread());
        processThread.interrupt();
    }

    static void appendJobExitStatus(final JobContext jobContext, final String status) {
        final String exitStatus = jobContext.getExitStatus();
        final String newStatus;
        if (exitStatus == null) {
            newStatus = status;
        } else {
            newStatus = exitStatus + " " + status;
        }
        jobContext.setExitStatus(newStatus);
    }
}
