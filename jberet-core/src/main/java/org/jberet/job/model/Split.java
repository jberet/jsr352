/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Corresponds to {@code jsl:Split} job element type in job XML.
 */
public final class Split extends AbstractJobElement {
    private static final long serialVersionUID = 4996794734335749303L;

    String next;

    /**
     * list of {@linkplain Flow flows} in this split.
     * Unlike a flow, which may contain all types of job-level job elements, split may only contain flows.
     */
    final List<Flow> flows = new ArrayList<Flow>();

    Split(final String id) {
        super(id);
    }

    /**
     * Gets the value of the {@code next} attribute, which specifies the next job element after this split completes.
     *
     * @return {@code next} attribute value
     */
    public String getAttributeNext() {
        return next;
    }

    /**
     * Sets the value of the {@code next} attribute.
     *
     * @param next {@code next} attribute value
     */
    void setAttributeNext(final String next) {
        this.next = next;
    }

    /**
     * Gets the list of {@linkplain Flow flows} contained in this split.
     *
     * @return list of {@linkplain Flow flows}
     */
    public List<Flow> getFlows() {
        return flows;
    }

    /**
     * Adds a {@link Flow} to this split.
     *
     * @param flow a flow to add to this split
     */
    void addFlow(final Flow flow) {
        flows.add(flow);
    }

    /**
     * Disables getting properties, since split contains no batch properties.
     *
     * @return no return
     * @throws IllegalStateException always
     */
    @Override
    public Properties getProperties() {
        throw new IllegalStateException();
    }

    /**
     * Disables setting properties, since split contains no batch properties.
     *
     * @param properties N/A
     * @throws IllegalStateException always
     */
    @Override
    public void setProperties(final Properties properties) {
        throw new IllegalStateException();
    }
}
