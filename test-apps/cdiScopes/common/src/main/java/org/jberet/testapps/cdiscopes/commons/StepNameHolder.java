/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.commons;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class holds step names. Typically caller of this class obtains
 * the holder collection by calling {@link #getStepNames()}, and adds
 * values to it. Other part of the application can access the holder
 * collection and verify the expected results.
 *
 * @since 1.3.0.Final
 */
public abstract class StepNameHolder {
    private final List<String> stepNames = new CopyOnWriteArrayList<String>();

    /**
     * Exposes the underlying holder list.
     * @return the underlying holder list
     */
    public List<String> getStepNames() {
        return stepNames;
    }
}
