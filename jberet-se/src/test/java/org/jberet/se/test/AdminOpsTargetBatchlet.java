/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

/**
 * The batchlet the administrative-operations tests target: it either completes at once or parks
 * until the test releases it, standing in for an execution that has stopped making progress.
 */
@Named
@Dependent
public class AdminOpsTargetBatchlet extends AbstractBatchlet implements Batchlet {

    private static final Logger LOGGER = Logger.getLogger(AdminOpsTargetBatchlet.class);

    public static final String DONE = "DONE";

    /** Counted down by the test to let every parked execution finish. */
    public static final CountDownLatch release = new CountDownLatch(1);

    /**
     * Counted down once by each execution as it reaches the parking point. A test that starts
     * {@code n} blocking executions sets this to {@code new CountDownLatch(n)} first and then
     * awaits it, so it never asserts against an execution that has not started yet.
     */
    public static volatile CountDownLatch running = new CountDownLatch(0);

    @Inject
    @BatchProperty(name = "block")
    private boolean block;

    @Override
    public String process() throws Exception {
        if (block) {
            LOGGER.info("AdminOpsTargetBatchlet parking until released");
            running.countDown();
            //the safety limit only matters if a test fails before releasing; it keeps the JVM exitable
            if (!release.await(5, TimeUnit.MINUTES)) {
                LOGGER.warn("AdminOpsTargetBatchlet was never released");
            }
        }
        return DONE;
    }
}
