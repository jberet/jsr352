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

package org.jberet.testapps.common;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;

@Named("L2")
public class JobListener2 implements JobListener {
    @Inject @BatchProperty(name="job-prop")
    private String jobProp = "L2";  //unmatched property

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp;  //nothing is injected

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referencedProp;  //nothing is injected

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        System.out.printf("In beforeJob of %s%n", this);
        //Assert.assertEquals("L2", jobProp);  should be null or "L2"?
        Assert.assertEquals(null, listenerProp);
        Assert.assertEquals(null, referencedProp);
        Assert.assertEquals(2, jobContext.getProperties().size());
        Assert.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
        System.out.printf("In afterJob of %s%n", this);
    }
}
