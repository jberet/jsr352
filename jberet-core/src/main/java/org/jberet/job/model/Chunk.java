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

import java.io.Serializable;

/**
 * Corresponds to jsl:Chunk job XML element type.
 */
public final class Chunk implements Serializable, Cloneable {
    private static final long serialVersionUID = 352707227911452807L;

    private static final String DEFAULT_CHECKPOINT_POLICY = "item";
    private static final int DEFAULT_ITEM_COUNT = 10;
    private static final int DEFAULT_LIMIT = 0;

    RefArtifact reader;
    RefArtifact processor;
    RefArtifact writer;
    RefArtifact checkpointAlgorithm;
    ExceptionClassFilter skippableExceptionClasses;
    ExceptionClassFilter retryableExceptionClasses;
    ExceptionClassFilter noRollbackExceptionClasses;
    String checkpointPolicy;

    //store these attributes as String since their value may be expressions that cannot parse to int or boolean
    String itemCount;
    String timeLimit;    //in seconds, default 0
    String skipLimit;    //default no limit
    String retryLimit;   //default no limit

    Chunk() {
    }

    /**
     * Gets the chunk's item reader as a {@link RefArtifact}.
     *
     * @return the chunk's item reader
     */
    public RefArtifact getReader() {
        return reader;
    }

    /**
     * Sets the chunk's item reader to a {@link RefArtifact}.
     *
     * @param reader the chunk's item reader {@code RefArtifact}
     */
    void setReader(final RefArtifact reader) {
        this.reader = reader;
    }

    /**
     * Gets the chunk's item processor as a {@link RefArtifact}.
     *
     * @return the chunk's item processor
     */
    public RefArtifact getProcessor() {
        return processor;
    }

    /**
     * Sets the chunk's item processor to a {@link RefArtifact}.
     *
     * @param processor the chunk's item processor {@code RefArtifact}
     */
    void setProcessor(final RefArtifact processor) {
        this.processor = processor;
    }

    /**
     * Gets the chunk's item writer as a {@link RefArtifact}.
     *
     * @return the chunk's item writer
     */
    public RefArtifact getWriter() {
        return writer;
    }

    /**
     * Sets the chunk's item writer to a {@link RefArtifact}.
     *
     * @param writer the chunk's item writer {@code RefArtifact}
     */
    void setWriter(final RefArtifact writer) {
        this.writer = writer;
    }

    /**
     * Gets the chunk's checkpoint algorithm as a {@link RefArtifact}.
     *
     * @return the chunk's checkpoint algorithm
     */
    public RefArtifact getCheckpointAlgorithm() {
        return checkpointAlgorithm;
    }

    /**
     * Sets the chunk's checkpoint algorithm to a {@link RefArtifact}.
     *
     * @param checkpointAlgorithm the chunk's checkpoint algorithm {@code RefArtifact}
     */
    void setCheckpointAlgorithm(final RefArtifact checkpointAlgorithm) {
        this.checkpointAlgorithm = checkpointAlgorithm;
    }

    /**
     * Gets the chunk's skippable exception classes filter.
     *
     * @return the chunk's skippable exception classes filter
     */
    public ExceptionClassFilter getSkippableExceptionClasses() {
        return skippableExceptionClasses;
    }

    /**
     * Sets the chunk's skippable exception classes filter.
     *
     * @param skippableExceptionClasses the chunk's skippable exception classes filter
     */
    void setSkippableExceptionClasses(final ExceptionClassFilter skippableExceptionClasses) {
        this.skippableExceptionClasses = skippableExceptionClasses;
    }

    /**
     * Gets the chunk's retryable exception classes filter.
     *
     * @return the chunk's retryable exception classes filter
     */
    public ExceptionClassFilter getRetryableExceptionClasses() {
        return retryableExceptionClasses;
    }

    /**
     * Sets the chunk's retryable exception classes filter.
     *
     * @param retryableExceptionClasses the chunk's retryable exception classes filter
     */
    void setRetryableExceptionClasses(final ExceptionClassFilter retryableExceptionClasses) {
        this.retryableExceptionClasses = retryableExceptionClasses;
    }

    /**
     * Gets the chunk's no rollback exception classes filter.
     *
     * @return the chunk's no rollback exception classes filter
     */
    public ExceptionClassFilter getNoRollbackExceptionClasses() {
        return noRollbackExceptionClasses;
    }

    /**
     * Sets the chunk's no rollback exception classes filter.
     *
     * @param noRollbackExceptionClasses the chunk's no rollback exception classes filter
     */
    void setNoRollbackExceptionClasses(final ExceptionClassFilter noRollbackExceptionClasses) {
        this.noRollbackExceptionClasses = noRollbackExceptionClasses;
    }

    /**
     * Gets the chunk's checkpoint policy, either "item" (default), or "custom".
     *
     * @return the chunk's checkpoint policy
     */
    public String getCheckpointPolicy() {
        return checkpointPolicy == null ? DEFAULT_CHECKPOINT_POLICY : checkpointPolicy;
    }

    /**
     * Sets the chunk's checkpoint policy.
     *
     * @param checkpointPolicy the chunk's checkpoint policy, either "item" (default), or "custom"
     */
    void setCheckpointPolicy(final String checkpointPolicy) {
        this.checkpointPolicy = checkpointPolicy;
    }

    /**
     * Gets the chunk's {@code item-count} attribute value as string.
     *
     * @return the chunk's {@code item-count} attribute value as string
     */
    public String getItemCount() {
        return itemCount;
    }

    /**
     * Gets the chunk's {@code item-count} attribute value as {@code int}.
     *
     * @return the chunk's {@code item-count} attribute value as int
     */
    public int getItemCountInt() {
        if (itemCount == null) {
            return DEFAULT_ITEM_COUNT;
        }
        return Integer.parseInt(itemCount);
    }

    /**
     * Sets the chunk's {@code item-count} attribute string value.
     *
     * @param itemCount the chunk's {@code item-count} string value
     */
    void setItemCount(final String itemCount) {
        if (itemCount != null) {
            this.itemCount = itemCount;
        }
    }

    /**
     * Gets the chunk's {@code time-limit} attribute value as string.
     *
     * @return the chunk's {@code time-limit} attribute value as string
     */
    public String getTimeLimit() {
        return timeLimit;
    }

    /**
     * Gets the chunk's {@code time-limit} attribute value as {@code int}.
     *
     * @return the chunk's {@code time-limit} attribute value as int
     */
    public int getTimeLimitInt() {
        if (timeLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Integer.parseInt(timeLimit);
    }

    /**
     * Sets the chunk's {@code time-limit} attribute value.
     *
     * @param timeLimit the chunk's {@code time-limit} string value
     */
    void setTimeLimit(final String timeLimit) {
        if (timeLimit != null) {
            this.timeLimit = timeLimit;
        }
    }

    /**
     * Gets the chunk's {@code skip-limit} attribute value.
     *
     * @return the chunk's {@code skip-limit} attribute value
     */
    public String getSkipLimit() {
        return skipLimit;
    }

    /**
     * Gets the chunk's {@code skip-limit} attribute value as {@code int}.
     *
     * @return the chunk's {@code skip-limit} attribute value as int
     */
    public int getSkipLimitInt() {
        if (skipLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Integer.parseInt(skipLimit);
    }

    /**
     * Sets the chunk's {@code skip-limit} attribute value.
     *
     * @param skipLimit the chunk's {@code skip-limit} attribute value
     */
    void setSkipLimit(final String skipLimit) {
        if (skipLimit != null) {
            this.skipLimit = skipLimit;
        }
    }

    /**
     * Gets the chunk's {@code retry-limit} attribute value.
     *
     * @return the chunk's {@code retry-limit} attribute value
     */
    public String getRetryLimit() {
        return retryLimit;
    }

    /**
     * Sets the chunk's {@code retry-limit} attribute value.
     *
     * @param retryLimit the chunk's {@code retry-limit} attribute value
     */
    void setRetryLimit(final String retryLimit) {
        if (retryLimit != null) {
            this.retryLimit = retryLimit;
        }
    }

    @Override
    protected Chunk clone() {
        final Chunk c = new Chunk();
        c.checkpointPolicy = this.checkpointPolicy;
        c.skipLimit = this.skipLimit;
        c.retryLimit = this.retryLimit;
        c.itemCount = this.itemCount;
        c.timeLimit = this.timeLimit;

        if (this.reader != null) {
            c.reader = this.reader.clone();
        }
        if (this.writer != null) {
            c.writer = this.writer.clone();
        }
        if (this.processor != null) {
            c.processor = this.processor.clone();
        }
        if (this.checkpointAlgorithm != null) {
            c.checkpointAlgorithm = this.checkpointAlgorithm.clone();
        }
        if (this.skippableExceptionClasses != null) {
            c.skippableExceptionClasses = this.skippableExceptionClasses.clone();
        }
        if (this.retryableExceptionClasses != null) {
            c.retryableExceptionClasses = this.retryableExceptionClasses;
        }
        if (this.noRollbackExceptionClasses != null) {
            c.noRollbackExceptionClasses = this.noRollbackExceptionClasses;
        }
        return c;
    }
}
