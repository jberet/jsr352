/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import static org.jberet.job.model.Properties.toJavaUtilProperties;

/**
 * Corresponds to {@code jsl:Step} job element type in job XML.
 */
public final class Step extends InheritableJobElement implements PropertiesHolder {
    private static final long serialVersionUID = 7699066774192733641L;
    private static final int DEFAULT_START_LIMIT = 0;

    String startLimit;  //default 0, no limit
    String allowStartIfComplete;  //default false
    String next;

    RefArtifact batchlet;
    Chunk chunk;
    Partition partition;

    Step(final String id) {
        super(id);
    }

    /**
     * Gets the step's {@code start-limit} attribute value as string.
     *
     * @return the step's {@code start-limit} attribute value as string
     */
    public String getStartLimit() {
        return startLimit;
    }

    /**
     * Gets the step's {@code start-limit} attribute value as {@code int}.
     *
     * @return the step's {@code start-limit} attribute value as {@code int}
     */
    public int getStartLimitInt() {
        if (startLimit == null) {
            return DEFAULT_START_LIMIT;
        }
        return Integer.parseInt(startLimit);
    }

    /**
     * Sets the step's {@code start-limit} attribute value.
     *
     * @param startLimit the step's {@code start-limit} attribute value as string
     */
    void setStartLimit(final String startLimit) {
        if (startLimit != null) {
            this.startLimit = startLimit;
        }
    }

    /**
     * Gets the step's {@code allow-start-if-complete} attribute value.
     *
     * @return the step's {@code allow-start-if-complete} attribute value as string
     */
    public String getAllowStartIfComplete() {
        return allowStartIfComplete;
    }

    /**
     * Gets the step's {@code allow-start-if-complete} attribute value as {@code boolean}.
     *
     * @return the step's {@code allow-start-if-complete} attribute value as {@code boolean}
     */
    public boolean getAllowStartIfCompleteBoolean() {
        return Boolean.parseBoolean(allowStartIfComplete);
    }

    /**
     * Sets the step's {@code allow-start-if-complete} attribute value
     *
     * @param allowStartIfComplete the step's {@code allow-start-if-complete} attribute string value
     */
    void setAllowStartIfComplete(final String allowStartIfComplete) {
        if (allowStartIfComplete != null) {
            this.allowStartIfComplete = allowStartIfComplete;
        }
    }

    /**
     * Gets the value of the {@code next} attribute, which specifies the next job element after this step completes.
     *
     * @return {@code next} attribute value (name of the next job element)
     */
    public String getAttributeNext() {
        return next;
    }

    /**
     * Sets the steps {@code next} attribute value, which specifies the next job element after this step completes.
     *
     * @param next name of the next job element
     */
    void setAttributeNext(final String next) {
        this.next = next;
    }

    /**
     * Gets the step's batchlet as a {@link RefArtifact}. For chunk-type step, this method returns {@code null}.
     *
     * @return the step's batchlet
     */
    public RefArtifact getBatchlet() {
        return batchlet;
    }

    /**
     * Sets the step's batchlet to a {@link RefArtifact}.
     *
     * @param batchlet the step's batchlet {@code RefArtifact}
     */
    void setBatchlet(final RefArtifact batchlet) {
        this.batchlet = batchlet;
    }

    /**
     * Gets the step's chunk element. For batchlet-type step, this method returns {@code null}.
     *
     * @return the step's chunk element
     */
    public Chunk getChunk() {
        return chunk;
    }

    /**
     * Sets the step's chunk element.
     *
     * @param chunk the step's chunk element
     */
    void setChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Gets the step's partition element.
     *
     * @return the step's partition element
     */
    public Partition getPartition() {
        return partition;
    }

    /**
     * Sets the step's partition element.
     *
     * @param partition the step's partition element
     */
    void setPartition(final Partition partition) {
        this.partition = partition;
    }

    public StepBuilder toBuilder() {
        final StepBuilder stepBuilder = new StepBuilder(this.getId());

        stepBuilder.properties(toJavaUtilProperties(this.getProperties()))
                   .startLimit(this.getStartLimitInt())
                   .allowStartIfComplete(this.getAllowStartIfCompleteBoolean())
                   .next(this.getAttributeNext());

        if (this.getBatchlet() != null) {
            stepBuilder.batchlet(this.getBatchlet().getRef(), toJavaUtilProperties(this.getBatchlet().getProperties()));
        }

        return stepBuilder;
    }
}
