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

/**
 * Corresponds to {@code jsl:Partition} job element type in job XML.
 */
public final class Partition implements Serializable, Cloneable {
    private static final long serialVersionUID = 1535154712638288876L;

    private RefArtifact mapper;
    private PartitionPlan plan;
    private RefArtifact collector;
    private RefArtifact analyzer;
    private RefArtifact reducer;

    Partition() {
    }

    /**
     * Gets the partition mapper.
     *
     * @return partition mapper as a {@code RefArtifact}
     */
    public RefArtifact getMapper() {
        return mapper;
    }

    /**
     * Sets the partition mapper.
     *
     * @param mapper partition mapper as a {@code RefArtifact}
     */
    void setMapper(final RefArtifact mapper) {
        this.mapper = mapper;
    }

    /**
     * Gets the {@linkplain PartitionPlan partition plan} for this partition.
     *
     * @return a {@code org.jberet.job.model.PartitionPlan}
     */
    public PartitionPlan getPlan() {
        return plan;
    }

    /**
     * Sets the {@linkplain PartitionPlan partition plan} for this partition.
     *
     * @param plan a {@code org.jberet.job.model.PartitionPlan}
     */
    void setPlan(final PartitionPlan plan) {
        this.plan = plan;
    }

    /**
     * Gets the partition collector.
     *
     * @return partition collector as a {@code RefArtifact}
     */
    public RefArtifact getCollector() {
        return collector;
    }

    /**
     * Sets the partition collector.
     *
     * @param collector partition collector as a {@code RefArtifact}
     */
    void setCollector(final RefArtifact collector) {
        this.collector = collector;
    }

    /**
     * Gets the partition analyzer.
     *
     * @return partition analyzer as a {@code RefArtifact}
     */
    public RefArtifact getAnalyzer() {
        return analyzer;
    }

    /**
     * Sets the partition analyzer.
     *
     * @param analyzer partition analyzer as a {@code RefArtifact}
     */
    void setAnalyzer(final RefArtifact analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Gets the partition reducer.
     *
     * @return partition reducer as a {@code RefArtifact}
     */
    public RefArtifact getReducer() {
        return reducer;
    }

    /**
     * Sets the partition reducer.
     *
     * @param reducer partition reducer as a {@code RefArtifact}
     */
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
