/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

abstract public class PostConstructPreDestroyBase {
    @Inject
    protected JobContext jobContext;

    @Inject
    protected StepContext stepContext;

    private boolean allowAddToJobExitStatus;  //set in job-level property add.to.job.exit.status in job xml

    @PostConstruct
    private void ps() {
        System.out.printf("PostConstructPreDestroyBase PostConstruct of %s%n", this);
        final String p = jobContext.getProperties().getProperty("add.to.job.exit.status");
        allowAddToJobExitStatus = Boolean.parseBoolean(p);
        addToJobExitStatus("PostConstructPreDestroyBase.ps");
    }

    @PreDestroy
    private void pd() {
        System.out.printf("PostConstructPreDestroyBase PreDestroy of %s%n", this);
        addToJobExitStatus("PostConstructPreDestroyBase.pd");
    }

    protected void addToJobExitStatus(final String s) {
        if (allowAddToJobExitStatus) {
            final String jes = jobContext.getExitStatus();
            if (jes == null) {
                jobContext.setExitStatus(s);
            } else {
                jobContext.setExitStatus(jes + " " + s);
            }
        }
    }
}
