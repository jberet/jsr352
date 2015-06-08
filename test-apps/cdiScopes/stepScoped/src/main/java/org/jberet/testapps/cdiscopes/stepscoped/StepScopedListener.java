/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.cdiscopes.stepscoped;

import java.util.List;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StepScopedListener implements StepListener {
    @Inject
    private Foo foo;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforeStep() throws Exception {
        foo.getStepNames().add(stepContext.getStepName());
        System.out.printf("In %s, foo.stepNames: %s%n", this, foo.getStepNames());
    }

    @Override
    public void afterStep() throws Exception {
        foo.getStepNames().add(stepContext.getStepName());

        System.out.printf("In %s, foo.stepNames: %s%n", this, foo.getStepNames());
        stepContext.setExitStatus(foo.getStepNames().toString());
    }
}
