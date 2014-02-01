/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.se.test;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SleepBatchlet extends AbstractBatchlet implements Batchlet {
    static final String INTERRUPTED = "Interrupted";
    static final String SLEPT = "Slept";

    @Inject
    @BatchProperty(name = "sleep.minutes")
    private int sleepMinutes;

    private Thread processThread;

    @Override
    public String process() throws Exception {
        processThread = Thread.currentThread();
        try {
            Thread.sleep(sleepMinutes * 60 * 1000);
        } catch (final InterruptedException e) {
            return INTERRUPTED;
        }
        return SLEPT;
    }

    @Override
    public void stop() throws Exception {
        System.out.printf("in @Stop, %s%n", Thread.currentThread());
        processThread.interrupt();
    }
}
