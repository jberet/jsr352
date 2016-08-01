/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.camel;

/**
 * This class holds constants identifying listener events during batch job execution.
 * The value of each event name typically matches the name of the batch listener method
 * that emits the event, except {@link #ON_CHUNK_ERROR}, which corresponds to
 * {@code javax.batch.api.chunk.listener.ChunkListener#onError(java.lang.Exception)}.
 * For instance, the value of {@link #BEFORE_JOB} equals to the name of method
 * {@code javax.batch.api.listener.JobListener#beforeJob()}.
 *
 * @see CamelJobListener
 * @see CamelStepListener
 * @see CamelChunkListener
 *
 * @since 1.3.0
 */
public final class EventType {
    private EventType() {
    }

    /**
     * The key for identifying the event type.
     */
    public static final String KEY = "eventType";

    // JobListener methods:
    //
    /**
     * The event type value for
     * {@code javax.batch.api.listener.JobListener#beforeJob()}.
     */
    public static final String BEFORE_JOB = "beforeJob";

    /**
     * The event type value for
     * {@code javax.batch.api.listener.JobListener#afterJob()}.
     */
    public static final String AFTER_JOB = "afterJob";


    // StepListener methods:
    //
    /**
     * The event type value for
     * {@code javax.batch.api.listener.StepListener#beforeStep()}.
     */
    public static final String BEFORE_STEP = "beforeStep";

    /**
     * The event type value for
     * {@code javax.batch.api.listener.StepListener#afterStep()}.
     */
    public static final String AFTER_STEP = "afterStep";


    // ChunkListener methods:
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ChunkListener#beforeChunk()}.
     */
    public static final String BEFORE_CHUNK = "beforeChunk";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ChunkListener#onError(java.lang.Exception)}.
     */
    public static final String ON_CHUNK_ERROR = "onChunkError";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ChunkListener#afterChunk()}.
     */
    public static final String AFTER_CHUNK = "afterChunk";


    // ItemProcessListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemProcessListener#beforeProcess(java.lang.Object)}.
     */
    public static final String BEFORE_PROCESS = "beforeProcess";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemProcessListener#afterProcess(java.lang.Object, java.lang.Object)}.
     */
    public static final String AFTER_PROCESS = "afterProcess";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemProcessListener#onProcessError(java.lang.Object, java.lang.Exception)}.
     */
    public static final String ON_PROCESS_ERROR = "onProcessError";


    // ItemReadListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemReadListener#beforeRead()}.
     */
    public static final String BEFORE_READ = "beforeRead";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemReadListener#afterRead(java.lang.Object)}.
     */
    public static final String AFTER_READ = "afterRead";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemReadListener#onReadError(java.lang.Exception)}.
     */
    public static final String ON_READ_ERROR = "onReadError";


    // ItemWriteListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemWriteListener#beforeWrite(java.util.List)}.
     */
    public static final String BEFORE_WRITE = "beforeWrite";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemWriteListener#afterWrite(java.util.List)}.
     */
    public static final String AFTER_WRITE = "afterWrite";

    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.ItemWriteListener#onWriteError(java.util.List, java.lang.Exception)}.
     */
    public static final String ON_WRITE_ERROR = "onWriteError";


    // RetryProcessListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.RetryProcessListener#onRetryProcessException(java.lang.Object, java.lang.Exception)}.
     */
    public static final String ON_RETRY_PROCESS_EXCEPTION = "onRetryProcessException";


    // RetryReadListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.RetryReadListener#onRetryReadException(java.lang.Exception)}.
     */
    public static final String ON_RETRY_READ_EXCEPTION = "onRetryReadException";


    // RetryWriteListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.RetryWriteListener#onRetryWriteException(java.util.List, java.lang.Exception)}.
     */
    public static final String ON_RETRY_WRITE_EXCEPTION = "onRetryWriteException";


    // SkipProcessListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.SkipProcessListener#onSkipProcessItem(java.lang.Object, java.lang.Exception)}.
     */
    public static final String ON_SKIP_PROCESS_ITEM = "onSkipProcessItem";


    // SkipReadListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.SkipReadListener#onSkipReadItem(java.lang.Exception)}.
     */
    public static final String ON_SKIP_READ_ITEM = "onSkipReadItem";


    // SkipWriteListener
    //
    /**
     * The event type value for
     * {@code javax.batch.api.chunk.listener.SkipWriteListener#onSkipWriteItem(java.util.List, java.lang.Exception)}.
     */
    public static final String ON_SKIP_WRITE_ITEM = "onSkipWriteItem";

}
