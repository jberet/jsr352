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

package org.jberet.support.io;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.StepListener;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * An implementation of {@code javax.batch.api.listener.StepListener} that validates transient user data in
 * {@code StepContext}.  It verifies that certain string values are found in {@code StepContext} transient user data,
 * and certain string values must not exist. Otherwise, {@code BatchRuntimeException} is thrown.
 */
@Named
final public class ValidatingStepListener implements StepListener {
    @Inject
    private StepContext stepContext;

    @Inject
    @BatchProperty
    private boolean validate;

    @Inject
    @BatchProperty
    private String[] expect;

    @Inject
    @BatchProperty
    private String[] forbid;

    @Override
    public void beforeStep() throws Exception {

    }

    @Override
    public void afterStep() throws Exception {
        if (!validate) {
            return;
        }
        final Object transientUserData = stepContext.getTransientUserData();
        if (transientUserData == null) {
            throw new BatchRuntimeException("transientUserData is not set.");
        }
        if (transientUserData instanceof String) {
            final String data = (String) transientUserData;
            System.out.printf("transientUserData:%n%s%n", data);
            if (expect != null) {
                for (final String s : expect) {
                    if (data.contains(s)) {
                        System.out.printf("Found expected string: %s%n", s);
                    } else {
                        throw new BatchRuntimeException("Expected string not found: " + s);
                    }
                }
            }
            if (forbid != null) {
                for (final String s : forbid) {
                    if (data.contains(s)) {
                        throw new BatchRuntimeException("Forbidden string found: " + s);
                    }
                    System.out.printf("Forbidden string not found: %s%n", s);
                }
            }
        } else {
            throw new BatchRuntimeException("transientUserData is of type " + transientUserData.getClass() +
            ", not of type String");
        }
    }
}
