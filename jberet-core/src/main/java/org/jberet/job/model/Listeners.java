/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

public class Listeners implements Serializable {
    private static final long serialVersionUID = 533293391099345352L;

    private List<RefArtifact> listeners = new ArrayList<RefArtifact>();

    private boolean merge = true;

    boolean isMerge() {
        return merge;
    }

    void setMerge(final String mergeVal) {
        if (mergeVal != null && !mergeVal.toLowerCase().equals("true")) {
            merge = false;
        }
    }

    public List<RefArtifact> getListeners() {
        return listeners;
    }

    public void setListeners(final List<RefArtifact> listeners) {
        this.listeners = listeners;
    }
}
