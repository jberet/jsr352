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

public final class Partition implements Serializable, Cloneable {
    private static final long serialVersionUID = 1535154712638288876L;

    private RefArtifact mapper;
    private PartitionPlan plan;
    private RefArtifact collector;
    private RefArtifact analyzer;
    private RefArtifact reducer;

    Partition() {
    }

    public RefArtifact getMapper() {
        return mapper;
    }

    void setMapper(final RefArtifact mapper) {
        this.mapper = mapper;
    }

    public PartitionPlan getPlan() {
        return plan;
    }

    void setPlan(final PartitionPlan plan) {
        this.plan = plan;
    }

    public RefArtifact getCollector() {
        return collector;
    }

    void setCollector(final RefArtifact collector) {
        this.collector = collector;
    }

    public RefArtifact getAnalyzer() {
        return analyzer;
    }

    void setAnalyzer(final RefArtifact analyzer) {
        this.analyzer = analyzer;
    }

    public RefArtifact getReducer() {
        return reducer;
    }

    void setReducer(final RefArtifact reducer) {
        this.reducer = reducer;
    }

    @Override
    protected Partition clone() {
        final Partition c = new Partition();
        if (this.mapper != null) {
            c.mapper = this.mapper.clone();
        }
        if (this.plan != null) {
            c.plan = this.plan.clone();
        }
        if (this.collector != null) {
            c.collector = this.collector.clone();
        }
        if (this.analyzer != null) {
            c.analyzer = this.analyzer.clone();
        }
        if (this.reducer != null) {
            c.reducer = this.reducer.clone();
        }
        return c;
    }
}
