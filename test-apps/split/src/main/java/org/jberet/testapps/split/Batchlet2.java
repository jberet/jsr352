/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.split;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;

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
