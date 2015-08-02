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
import java.util.List;

/**
 * A common interface for all job elements: decision, flow, split and step.
 *
 * @see Step
 * @see Split
 * @see Flow
 * @see Decision
 */
public interface JobElement extends Serializable {
    /**
     * Gets the id of this job element.
     *
     * @return id of this job element
     */
    String getId();

    /**
     * Gets the list of transition elements, such as {@link org.jberet.job.model.Transition.End},
     * {@link org.jberet.job.model.Transition.Fail}, {@link org.jberet.job.model.Transition.Next},
     * {@link org.jberet.job.model.Transition.Stop}.
     *
     * @return list of transition elements
     */
    List<Transition> getTransitionElements();

    /**
     * Adds a {@code org.jberet.job.model.Transition} to the list of transition elements of this job element.
     *
     * @param transition a {@code org.jberet.job.model.Transition}
     */
    void addTransitionElement(Transition transition);
}
