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
 
package org.jberet.job.model;

import java.util.ArrayList;
import java.util.List;

public final class SplitBuilder {
    private final String id;
    private String next;
    private final List<Flow> flows = new ArrayList<Flow>();

    public SplitBuilder(final String id) {
        this.id = id;
    }

    public SplitBuilder next(final String next) {
        this.next = next;
        return this;
    }

    public SplitBuilder flow(final Flow flow) {
        flows.add(flow);
        return this;
    }

    public Split build() {
        final Split split = new Split(id);
        split.next = next;

        for (final Flow f : flows) {
            split.flows.add(f);
        }
        return split;
    }
}
