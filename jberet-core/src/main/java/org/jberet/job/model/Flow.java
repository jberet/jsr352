/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.job.model;

import java.util.ArrayList;
import java.util.List;

public final class Flow extends InheritableJobElement {
    private static final long serialVersionUID = 6569569970633169427L;

    String next;

    List<JobElement> jobElements = new ArrayList<JobElement>();

    Flow(final String id) {
        super(id);
    }

    public String getAttributeNext() {
        return next;
    }

    public List<JobElement> getJobElements() {
        return jobElements;
    }

    @Override
    public Listeners getListeners() {
        throw new IllegalStateException();
    }

    @Override
    public void setListeners(final Listeners listeners) {
        throw new IllegalStateException();
    }

    @Override
    public Properties getProperties() {
        throw new IllegalStateException();
    }

    @Override
    public void setProperties(final Properties properties) {
        throw new IllegalStateException();
    }
}
