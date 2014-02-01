/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.se.test;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.JobListener;
import javax.batch.operations.BatchRuntimeException;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public final class FailJobListener implements JobListener {
    @Inject
    @BatchProperty
    private boolean failBeforeJob;

    @Inject
    @BatchProperty
    private boolean failAfterJob;

    @Override
    public void beforeJob() throws Exception {
        if (failBeforeJob) {
            throw new BatchRuntimeException("failBeforeJob is set to " + failBeforeJob);
        }
        System.out.printf("In beforeJob method of %s%n", this);
    }

    @Override
    public void afterJob() throws Exception {
        if (failAfterJob) {
            throw new BatchRuntimeException("failAfterJob is set to " + failAfterJob);
        }
        System.out.printf("In afterJob method of %s%n", this);
    }
}
