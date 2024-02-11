/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Assertions;

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
        //Assertions.assertEquals("L2", jobProp);  should be null or "L2"?
        Assertions.assertEquals(null, listenerProp);
        Assertions.assertEquals(null, referencedProp);
        Assertions.assertEquals(2, jobContext.getProperties().size());
        Assertions.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
        System.out.printf("In afterJob of %s%n", this);
    }
}
