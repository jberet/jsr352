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
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;

@Named("L1")
public class JobListener1 extends AbstractJobListener implements JobListener {
    @Inject @BatchProperty(name="job-prop")
    private String jobProp;  //nothing is injected

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp;  //injected

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referenceJobProp;

    @Inject @BatchProperty(name="reference-job-param")
    private String referenceJobParam;

    @Inject @BatchProperty(name="reference-system-property")
    private String referenceSystemProperty;

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        System.out.printf("In beforeJob of %s%n", this);
        Assert.assertEquals(null, jobProp);
        Assert.assertEquals("listener-prop", listenerProp);
        Assert.assertEquals("job-prop", referenceJobProp);
        Assert.assertEquals("job-param", referenceJobParam);
        Assert.assertEquals(System.getProperty("java.version"), referenceSystemProperty);

        Assert.assertEquals(2, jobContext.getProperties().size());
        Assert.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
        System.out.printf("In afterJob of %s%n", this);
    }
}
