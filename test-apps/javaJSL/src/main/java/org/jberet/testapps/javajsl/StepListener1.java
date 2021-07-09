/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.javajsl;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class StepListener1 implements StepListener {
    @Inject
    @BatchProperty
    private String stepListenerk1;

    @Inject
    @BatchProperty
    private String stepListenerk2;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforeStep() throws Exception {
        System.out.printf("In beforeStep of %s%n", this);
    }

    @Override
    public void afterStep() throws Exception {
        System.out.printf("In afterStep of %s%n", this);
        stepContext.setExitStatus(stepContext.getExitStatus() + stepListenerk1 + stepListenerk2);
    }
}
