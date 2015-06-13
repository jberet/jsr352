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

package org.jberet.testapps.javajsl;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class JobListener1 implements JobListener {
    @Inject
    @BatchProperty
    private String jobListenerk1;

    @Inject
    @BatchProperty
    private String jobListenerk2;

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        System.out.printf("In beforeJob of %s%n", this);
    }

    @Override
    public void afterJob() throws Exception {
        System.out.printf("In afterJob of %s%n", this);
        jobContext.setExitStatus(jobListenerk1 + jobListenerk2);
    }
}
