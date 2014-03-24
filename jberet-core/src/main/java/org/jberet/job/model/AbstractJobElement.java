/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(final Properties properties) {
        this.properties = properties;
    }
}
