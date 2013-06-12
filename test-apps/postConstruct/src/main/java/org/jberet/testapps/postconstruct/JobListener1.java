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

package org.jberet.testapps.postconstruct;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.batch.api.listener.JobListener;
import javax.batch.operations.BatchRuntimeException;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.common.PostConstructPreDestroyBase;

@Named
public class JobListener1 extends PostConstructPreDestroyBase implements JobListener {
    @Inject @BatchProperty(name = "os.name")
    private String osName;

    @Override
    public void beforeJob() throws Exception {
        addToJobExitStatus("JobListener1.beforeJob");
    }

    @Override
    public void afterJob() throws Exception {
        addToJobExitStatus("JobListener1.afterJob");
    }

    @PostConstruct
    public void ps() {
        System.out.printf("JobListener1 PostConstruct of %s%n", this);
        if (osName == null) {
            throw new BatchRuntimeException("osNmae field has not been initialized when checking from PostConstruct method.");
        }
        addToJobExitStatus("JobListener1.ps");
    }

    @PreDestroy
    public void pd() {
        System.out.printf("JobListener1 PreDestroy of %s%n", this);
        addToJobExitStatus("JobListener1.pd");
    }
}
