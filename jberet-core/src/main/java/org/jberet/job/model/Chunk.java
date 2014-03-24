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

    public RefArtifact getReader() {
        return reader;
    }

    void setReader(final RefArtifact reader) {
        this.reader = reader;
    }

    public RefArtifact getProcessor() {
        return processor;
    }

    void setProcessor(final RefArtifact processor) {
        this.processor = processor;
    }

    public RefArtifact getWriter() {
        return writer;
    }

    void setWriter(final RefArtifact writer) {
        this.writer = writer;
    }

    public RefArtifact getCheckpointAlgorithm() {
        return checkpointAlgorithm;
    }

    void setCheckpointAlgorithm(final RefArtifact checkpointAlgorithm) {
        this.checkpointAlgorithm = checkpointAlgorithm;
    }

    public ExceptionClassFilter getSkippableExceptionClasses() {
        return skippableExceptionClasses;
    }

    void setSkippableExceptionClasses(final ExceptionClassFilter skippableExceptionClasses) {
        this.skippableExceptionClasses = skippableExceptionClasses;
    }

    public ExceptionClassFilter getRetryableExceptionClasses() {
        return retryableExceptionClasses;
    }

    void setRetryableExceptionClasses(final ExceptionClassFilter retryableExceptionClasses) {
        this.retryableExceptionClasses = retryableExceptionClasses;
    }

    public ExceptionClassFilter getNoRollbackExceptionClasses() {
        return noRollbackExceptionClasses;
    }

    void setNoRollbackExceptionClasses(final ExceptionClassFilter noRollbackExceptionClasses) {
        this.noRollbackExceptionClasses = noRollbackExceptionClasses;
    }

    public String getCheckpointPolicy() {
        return checkpointPolicy == null ? DEFAULT_CHECKPOINT_POLICY : checkpointPolicy;
    }

    void setCheckpointPolicy(final String checkpointPolicy) {
        this.checkpointPolicy = checkpointPolicy;
    }

    public String getItemCount() {
        return itemCount;
    }

    public int getItemCountInt() {
        if (itemCount == null) {
            return DEFAULT_ITEM_COUNT;
        }
        return Integer.parseInt(itemCount);
    }

    void setItemCount(final String itemCount) {
        if (itemCount != null) {
            this.itemCount = itemCount;
        }
    }

    public String getTimeLimit() {
        return timeLimit;
    }

    public int getTimeLimitInt() {
        if (timeLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Integer.parseInt(timeLimit);
    }

    void setTimeLimit(final String timeLimit) {
        if (timeLimit != null) {
            this.timeLimit = timeLimit;
        }
    }

    public String getSkipLimit() {
        return skipLimit;
    }

    public int getSkipLimitInt() {
        if (skipLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Integer.parseInt(skipLimit);
    }

    void setSkipLimit(final String skipLimit) {
        if (skipLimit != null) {
            this.skipLimit = skipLimit;
        }
    }

    public String getRetryLimit() {
        return retryLimit;
    }

    public int getRetryLimitInt() {
        if (retryLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Integer.parseInt(retryLimit);
    }

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
