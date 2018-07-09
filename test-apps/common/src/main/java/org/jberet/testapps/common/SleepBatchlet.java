/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A batchlet that sleeps periodically. The amount of time it sleeps each time
 * is configured with {@code sleepMillis} batch property. The number of sleeps
 * is configured with {@code sleepCount} batch property. Before each sleep, it
 * checks whether this batchlet has been requested to stop, and if so, it exits
 * the {@link #process()} method immediately.
 * <p>
 * If this batchlet did not sleep, the {@link #process()} method returns "NO SLEEP".
 * If it slept, the {@link #process()} method returns "SLEPT xxx" (xxx is the number
 * of milliseconds).
 *
 * @since 1.3.0.Beta7
 */
@Named
public class SleepBatchlet implements Batchlet {
    @Inject
    @BatchProperty
    private long sleepMillis;

    @Inject
    @BatchProperty
    private int sleepCount;

    private AtomicBoolean stopRequested = new AtomicBoolean(false);

    @Override
    public String process() throws Exception {
        if (sleepMillis == 0) {
            return "NO SLEEP";
        }
        if (sleepCount == 0) {
            sleepCount = 1;
        }

        long duration = 0;
        for (int i = 0; i < sleepCount; i++) {
            if (stopRequested.get()) {
                break;
            }
            Thread.sleep(sleepMillis);
            duration += sleepMillis;
        }
        System.out.printf("SleepBatchlet slept for %s milliseconds%n", duration);
        return "SLEPT " + duration;
    }

    @Override
    public void stop() throws Exception {
        stopRequested.set(true);
    }
}
