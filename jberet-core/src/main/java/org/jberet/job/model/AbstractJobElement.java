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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for job element types such as job, step, flow, split and decision.
 *
 * @see Job
 * @see Step
 * @see Decision
 * @see Flow
 * @see Split
 * @see InheritableJobElement
 */
abstract class AbstractJobElement implements JobElement, Serializable {
    private static final long serialVersionUID = -8396145727646776440L;

    final String id;
    private Properties properties;

    /**
     * Transition elements in the same order as they appear in job xml.  Flow, decision and step have transition
     * elements, but split does not.
     */
    private final List<Transition> transitions = new ArrayList<Transition>();

    AbstractJobElement(final String id) {
        this.id = id;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public List<Transition> getTransitionElements() {
        return transitions;
    }

    @Override
    public void addTransitionElement(final Transition transition) {
        transitions.add(transition);
    }

    /**
     * Gets the {@code org.jberet.job.model.Properties} contained in this job element.
     *
     * @return the {@code org.jberet.job.model.Properties} contained in this job element
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the {@code org.jberet.job.model.Properties} contained in this job element.
     *
     * @param properties {@code org.jberet.job.model.Properties} for this job element
     */
    public void setProperties(final Properties properties) {
        this.properties = properties;
    }
}
