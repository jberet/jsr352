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

import java.util.ArrayList;
import java.util.List;

public final class Step extends AbstractJobElement {
    private static final long serialVersionUID = 7699066774192733641L;

    private int startLimit;  //default 0, no limit
    private boolean allowStartIfComplete;  //default false
    private String next;

    private Properties properties;
    private final List<RefArtifact> listeners = new ArrayList<RefArtifact>();
    private RefArtifact batchlet;
    private Chunk chunk;
    private Partition partition;

    Step(String id) {
        super(id);
    }

    public int getStartLimit() {
        return startLimit;
    }

    void setStartLimit(String startLimit) {
        if (startLimit != null) {
            this.startLimit = Integer.parseInt(startLimit);
        }
    }

    public boolean getAllowStartIfComplete() {
        return allowStartIfComplete;
    }

    void setAllowStartIfComplete(String allowStartIfComplete) {
        if (allowStartIfComplete != null) {
            this.allowStartIfComplete = Boolean.parseBoolean(allowStartIfComplete);
        }
    }

    public String getAttributeNext() {
        return next;
    }

    void setAttributeNext(String next) {
        this.next = next;
    }

    public Properties getProperties() {
        return properties;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<RefArtifact> getListeners() {
        return listeners;
    }

    void addListener(RefArtifact listener) {
        listeners.add(listener);
    }

    void addListeners(List<RefArtifact> ls) {
        listeners.addAll(ls);
    }

    public RefArtifact getBatchlet() {
        return batchlet;
    }

    void setBatchlet(RefArtifact batchlet) {
        this.batchlet = batchlet;
    }

    public Chunk getChunk() {
        return chunk;
    }

    void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public Partition getPartition() {
        return partition;
    }

    void setPartition(Partition partition) {
        this.partition = partition;
    }
}
