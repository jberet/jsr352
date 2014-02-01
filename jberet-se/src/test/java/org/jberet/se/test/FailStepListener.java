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
import javax.batch.operations.BatchRuntimeException;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public final class FailStepListener implements javax.batch.api.listener.StepListener {
    @Inject
    @BatchProperty
    private boolean failBeforeStep;

    @Inject
    @BatchProperty
    private boolean failAfterStep;
    
    @Override
    public void beforeStep() throws Exception {
        if (failBeforeStep) {
            throw new BatchRuntimeException("failBeforeStep is set to " + failBeforeStep);
        }
        System.out.println("In beforeStep method of " + this);
    }

    @Override
    public void afterStep() throws Exception {
        if (failAfterStep) {
            throw new BatchRuntimeException("failAfterStep is set to " + failAfterStep);
        }
        System.out.println("In afterStep method of " + this);
    }
}
