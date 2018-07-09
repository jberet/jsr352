/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
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
 * Corresponds to {@code jsl:Listeners} job XML element type.
 */
public class Listeners extends MergeableElement implements Serializable {
    private static final long serialVersionUID = 533293391099345352L;

    private List<RefArtifact> listeners = new ArrayList<RefArtifact>();

    /**
     * Gets the list of listeners as {@code List<RefArtifact>}.
     *
     * @return list of listeners
     */
    public List<RefArtifact> getListeners() {
        return listeners;
    }

    /**
     * Sets the list of listeners.
     *
     * @param listeners list of listeners
     */
    public void setListeners(final List<RefArtifact> listeners) {
        this.listeners = listeners;
    }
}
