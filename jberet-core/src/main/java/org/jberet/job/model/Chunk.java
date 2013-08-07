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

public final class Chunk implements Serializable {
    private static final long serialVersionUID = 352707227911452807L;

    private static final String DEFAULT_CHECKPOINT_POLICY = "item";
    private static final int DEFAULT_ITEM_COUNT = 10;

    private RefArtifact reader;
    private RefArtifact processor;
    private RefArtifact writer;
    private RefArtifact checkpointAlgorithm;
    private ExceptionClassFilter skippableExceptionClasses;
    private ExceptionClassFilter retryableExceptionClasses;
    private ExceptionClassFilter noRollbackExceptionClasses;

    private String checkpointPolicy = DEFAULT_CHECKPOINT_POLICY;
    private int itemCount = DEFAULT_ITEM_COUNT;
    private int timeLimit;  //in seconds, default 0
    private int skipLimit;  //default no limit
    private int retryLimit; //default no limit

    Chunk() {
    }

    public static String getDefaultCheckpointPolicy() {
        return DEFAULT_CHECKPOINT_POLICY;
    }

    public static int getDefaultItemCount() {
        return DEFAULT_ITEM_COUNT;
    }

    public RefArtifact getReader() {
        return reader;
    }

    void setReader(RefArtifact reader) {
        this.reader = reader;
    }

    public RefArtifact getProcessor() {
        return processor;
    }

    void setProcessor(RefArtifact processor) {
        this.processor = processor;
    }

    public RefArtifact getWriter() {
        return writer;
    }

    void setWriter(RefArtifact writer) {
        this.writer = writer;
    }

    public RefArtifact getCheckpointAlgorithm() {
        return checkpointAlgorithm;
    }

    void setCheckpointAlgorithm(RefArtifact checkpointAlgorithm) {
        this.checkpointAlgorithm = checkpointAlgorithm;
    }

    public ExceptionClassFilter getSkippableExceptionClasses() {
        return skippableExceptionClasses;
    }

    void setSkippableExceptionClasses(ExceptionClassFilter skippableExceptionClasses) {
        this.skippableExceptionClasses = skippableExceptionClasses;
    }

    public ExceptionClassFilter getRetryableExceptionClasses() {
        return retryableExceptionClasses;
    }

    void setRetryableExceptionClasses(ExceptionClassFilter retryableExceptionClasses) {
        this.retryableExceptionClasses = retryableExceptionClasses;
    }

    public ExceptionClassFilter getNoRollbackExceptionClasses() {
        return noRollbackExceptionClasses;
    }

    void setNoRollbackExceptionClasses(ExceptionClassFilter noRollbackExceptionClasses) {
        this.noRollbackExceptionClasses = noRollbackExceptionClasses;
    }

    public String getCheckpointPolicy() {
        return checkpointPolicy;
    }

    void setCheckpointPolicy(String checkpointPolicy) {
        this.checkpointPolicy = checkpointPolicy;
    }

    public int getItemCount() {
        return itemCount;
    }

    void setItemCount(String itemCount) {
        if (itemCount != null) {
            this.itemCount = Integer.parseInt(itemCount);
        }
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    void setTimeLimit(String timeLimit) {
        if (timeLimit != null) {
            this.timeLimit = Integer.parseInt(timeLimit);
        }
    }

    public int getSkipLimit() {
        return skipLimit;
    }

    void setSkipLimit(String skipLimit) {
        if (skipLimit != null) {
            this.skipLimit = Integer.parseInt(skipLimit);
        }
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    void setRetryLimit(String retryLimit) {
        if (retryLimit != null) {
            this.retryLimit = Integer.parseInt(retryLimit);
        }
    }
}
