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

package org.jberet.testapps.common;

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
