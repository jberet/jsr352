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
import jakarta.batch.api.listener.AbstractJobListener;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Assertions;

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
        Assertions.assertEquals(null, jobProp);
        Assertions.assertEquals("listener-prop", listenerProp);
        Assertions.assertEquals("job-prop", referenceJobProp);
        Assertions.assertEquals("job-param", referenceJobParam);
        Assertions.assertEquals(System.getProperty("java.version"), referenceSystemProperty);

        Assertions.assertEquals(2, jobContext.getProperties().size());
        Assertions.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
        System.out.printf("In afterJob of %s%n", this);
    }
}
