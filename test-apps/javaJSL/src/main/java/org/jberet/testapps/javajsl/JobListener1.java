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

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;

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
