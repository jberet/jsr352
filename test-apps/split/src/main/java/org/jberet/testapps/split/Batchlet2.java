/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.split;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class Batchlet2 extends AbstractBatchlet {
    @Inject
    @BatchProperty
    private String stepExitStatus;

    @Inject
    @BatchProperty
    private int sleepSeconds;

    @Inject
    @BatchProperty
    private boolean fail;

    @Override
    public String process() throws Exception {
        if (fail) {
            throw new RuntimeException("Configured to fail " + this);
        }
        if (sleepSeconds > 0) {
            Thread.sleep(sleepSeconds * 1000);
        }
        return stepExitStatus;
    }
}
