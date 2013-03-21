/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.mybatch.testapps.loadBatchXml;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.junit.Assert;

public class StepListener4 extends AbstractStepListener implements StepListener {
    @Inject @BatchProperty(name="step-prop")
    private String stepProp;  //nothing is injected

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp = "default";  //nothing to inject, keep the field default value

    @Inject @BatchProperty(name = "reference-job-prop")
    private Object referencedProp;  //nothing to inject

    @Inject @BatchProperty(name = "reference-step-prop")
    private Object referencedStepProp;  //nothing to inject

    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforeStep() throws Exception {
        System.out.printf("In beforeStep of %s%n", this);
        Assert.assertEquals(null, stepProp);
        Assert.assertEquals("default", listenerProp);
        Assert.assertEquals(null, referencedProp);
        Assert.assertEquals(null, referencedStepProp);

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
