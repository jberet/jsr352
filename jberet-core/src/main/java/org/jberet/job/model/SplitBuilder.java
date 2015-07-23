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

/**
 * Builder class for building a single {@link Split}. After the split is built, the same {@code SplitBuilder}
 * instance should not be reused to build another split.
 * <p/>
 * This class does not support multi-threaded access or modification. Usage example:
 * <p/>
 * <pre>
 *      Split split = new SplitBuilder(splitName)
 *              .flow(new FlowBuilder(flowName)
 *                      .step(new StepBuilder(stepName).batchlet(batchlet1Name).build())
 *                      .build())
 *              .flow(new FlowBuilder(flow2Name)
 *                      .step(new StepBuilder(step2Name).batchlet(batchlet1Name).build())
 *                      .build())
 *              .next(step3Name)
 *              .build())
 * </pre>
 *
 * @see JobBuilder
 * @see FlowBuilder
 * @see DecisionBuilder
 * @see StepBuilder
 *
 * @since 1.2.0
 */
public final class SplitBuilder {
    private final String id;
    private String next;
    private final List<Flow> flows = new ArrayList<Flow>();

    /**
     * Constructs the {@code SplitBuilder} instance for building the {@linkplain Split split} with the specified {@code id}.
     *
     * @param id split id
     */
    public SplitBuilder(final String id) {
        this.id = id;
    }

    /**
     * Sets the {@code next} attribute value for the split.
     *
     * @param next id of the next job element after the split
     * @return this {@code SplitBuilder}
     */
    public SplitBuilder next(final String next) {
        this.next = next;
        return this;
    }

    /**
     * Adds a {@linkplain Flow flow} to the split.
     *
     * @param flow the flow to be added to the split
     * @return this {@code SplitBuilder}
     */
    public SplitBuilder flow(final Flow flow) {
        flows.add(flow);
        return this;
    }

    /**
     * Builds the {@linkplain Split split}.
     *
     * @return the split built with this {@code SplitBuilder}
     */
    public Split build() {
        final Split split = new Split(id);
        split.next = next;

        for (final Flow f : flows) {
            split.flows.add(f);
        }
        return split;
    }
}
