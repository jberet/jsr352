/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
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

public final class Step extends InheritableJobElement implements PropertiesHolder {
    private static final long serialVersionUID = 7699066774192733641L;
    private static final int DEFAULT_START_LIMIT = 0;

    private String startLimit;  //default 0, no limit
    private String allowStartIfComplete;  //default false
    private String next;

    private RefArtifact batchlet;
    private Chunk chunk;
    private Partition partition;

    Step(final String id) {
        super(id);
    }

    public String getStartLimit() {
        return startLimit;
    }

    public int getStartLimitInt() {
        if (startLimit == null) {
            return DEFAULT_START_LIMIT;
        }
        return Integer.parseInt(startLimit);
    }

    void setStartLimit(final String startLimit) {
        if (startLimit != null) {
            this.startLimit = startLimit;
        }
    }

    public String getAllowStartIfComplete() {
        return allowStartIfComplete;
    }

    public boolean getAllowStartIfCompleteBoolean() {
        return Boolean.parseBoolean(allowStartIfComplete);
    }

    void setAllowStartIfComplete(final String allowStartIfComplete) {
        if (allowStartIfComplete != null) {
            this.allowStartIfComplete = allowStartIfComplete;
        }
    }

    public String getAttributeNext() {
        return next;
    }

    void setAttributeNext(final String next) {
        this.next = next;
    }

    public RefArtifact getBatchlet() {
        return batchlet;
    }

    void setBatchlet(final RefArtifact batchlet) {
        this.batchlet = batchlet;
    }

    public Chunk getChunk() {
        return chunk;
    }

    void setChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    public Partition getPartition() {
        return partition;
    }

    void setPartition(final Partition partition) {
        this.partition = partition;
    }
}
