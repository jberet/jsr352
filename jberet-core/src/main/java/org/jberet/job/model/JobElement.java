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
 */
public interface JobElement extends Serializable {
    String getId();

    List<Transition> getTransitionElements();

    void addTransitionElement(Transition transition);
}
