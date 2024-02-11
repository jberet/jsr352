/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.loadBatchXml;

import jakarta.inject.Inject;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import org.junit.jupiter.api.Assertions;

public class StepListener4 extends AbstractStepListener implements StepListener {
    @Inject @BatchProperty(name="step-prop")
    private String stepProp;  //nothing is injected

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp = "default";  //unmatched property

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referencedProp;  //nothing to inject

    @Inject @BatchProperty(name = "reference-step-prop")
    private String referencedStepProp;  //nothing to inject

    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforeStep() throws Exception {
        System.out.printf("In beforeStep of %s%n", this);
        Assertions.assertEquals(null, stepProp);
        Assertions.assertEquals("default", listenerProp);
        Assertions.assertEquals(null, referencedProp);
        Assertions.assertEquals(null, referencedStepProp);

        Assertions.assertEquals(2, jobContext.getProperties().size());
        Assertions.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));

        Assertions.assertEquals(2, stepContext.getProperties().size());
        Assertions.assertEquals("step-prop", stepContext.getProperties().get("step-prop"));
    }

    @Override
    public void afterStep() throws Exception {
        System.out.printf("In afterStep of %s%n", this);
    }
}
