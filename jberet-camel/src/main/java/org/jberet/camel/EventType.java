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
 * @since 1.3.0
 */
public final class EventType {
    private EventType() {
    }

    /**
     * The key of the message header to indicate whether the event is for
     * before job or after job execution.
     */
    public static final String EVENT_TYPE = "eventType";
    /**
     * The value of the message header to indicate that the event is for
     * before job execution.
     */
    public static final String BEFORE_JOB = "beforeJob";
    /**
     * The value of the message header to indicate that the event is for
     * after job execution.
     */
    public static final String AFTER_JOB = "afterJob";

    /**
     * The value of the message header to indicate that the event is for
     * before step execution.
     */
    public static final String BEFORE_STEP = "beforeStep";

    /**
     * The value of the message header to indicate that the event is for
     * after step execution.
     */
    public static final String AFTER_STEP = "afterStep";


    // ChunkListener methods:
    public static final String BEFORE_CHUNK = "beforeChunk";

    public static final String ON_CHUNK_ERROR = "onChunkError";

    public static final String AFTER_CHUNK = "afterChunk";


    // ItemProcessListener
    public static final String BEFORE_PROCESS = "beforeProcess";

    public static final String AFTER_PROCESS = "afterProcess";

    public static final String ON_PROCESS_ERROR = "onProcessError";


    // ItemReadListener
    public static final String BEFORE_READ = "beforeRead";

    public static final String AFTER_READ = "afterRead";

    public static final String ON_READ_ERROR = "onReadError";


    // ItemWriteListener
    public static final String BEFORE_WRITE = "beforeWrite";

    public static final String AFTER_WRITE = "afterWrite";

    public static final String ON_WRITE_ERROR = "onWriteError";


    // RetryProcessListener
    public static final String ON_RETRY_PROCESS_EXCEPTION = "onRetryProcessException";


    // RetryReadListener
    public static final String ON_RETRY_READ_EXCEPTION = "onRetryReadException";


    // RetryWriteListener
    public static final String ON_RETRY_WRITE_EXCEPTION = "onRetryWriteException";


    // SkipProcessListener
    public static final String ON_SKIP_PROCESS_ITEM = "onSkipProcessItem";


    // SkipReadListener
    public static final String ON_SKIP_READ_ITEM = "onSkipReadItem";


    // SkipWriteListener
    public static final String ON_SKIP_WRITE_ITEM = "onSkipWriteItem";

}
