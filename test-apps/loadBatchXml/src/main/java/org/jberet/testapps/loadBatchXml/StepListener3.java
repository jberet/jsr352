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

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.junit.Assert;

public class StepListener3 extends AbstractStepListener implements StepListener {
    @Inject @BatchProperty(name="step-prop")
    private String stepProp;  //nothing is injected

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp;  //injected

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referencedProp;

    @Inject @BatchProperty(name = "reference-step-prop")
    private String referencedStepProp;

    @Inject @BatchProperty(name="reference-job-prop-2")
    private String referenceJobProp2;

    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforeStep() throws Exception {
        System.out.printf("In beforeStep of %s%n", this);
        Assert.assertEquals(null, stepProp);
        Assert.assertEquals("step-listener-prop", listenerProp);
        Assert.assertEquals("job-prop", referencedProp);
        Assert.assertEquals("step-prop", referencedStepProp);
        Assert.assertEquals("step-prop-2", referenceJobProp2);

        Assert.assertEquals(2, jobContext.getProperties().size());
        Assert.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));

        Assert.assertEquals(2, stepContext.getProperties().size());
        Assert.assertEquals("step-prop", stepContext.getProperties().get("step-prop"));

    }

    @Override
    public void afterStep() throws Exception {
        System.out.printf("In afterStep of %s%n", this);
    }
}
