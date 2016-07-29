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

import java.util.List;
import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import static org.jberet.camel.EventType.*;

@Named
public class CamelChunkListener extends CamelListenerBase
        implements ChunkListener, ItemProcessListener, ItemReadListener, ItemWriteListener,
        RetryProcessListener, RetryReadListener, RetryWriteListener, SkipProcessListener,
        SkipReadListener, SkipWriteListener {

    /**
     * Injection of {@code javax.batch.runtime.context.StepContext} by batch
     * runtime.
     */
    @Inject
    protected StepContext stepContext;

    // ChunkListener methods:
    @Override
    public void beforeChunk() throws Exception {
        sendBodyAndHeader(BEFORE_CHUNK, null, null, null, null);
    }

    @Override
    public void onError(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_CHUNK_ERROR, null, null, null, ex);
    }

    @Override
    public void afterChunk() throws Exception {
        sendBodyAndHeader(AFTER_CHUNK, null, null, null, null);
    }

    // ItemProcessListener
    @Override
    public void beforeProcess(final Object item) throws Exception {
        sendBodyAndHeader(BEFORE_PROCESS, item, null, null, null);
    }

    @Override
    public void afterProcess(final Object item, final Object result) throws Exception {
        sendBodyAndHeader(AFTER_PROCESS, item, result, null, null);
    }

    @Override
    public void onProcessError(final Object item, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_PROCESS_ERROR, item, null, null, ex);
    }

    // ItemReadListener
    @Override
    public void beforeRead() throws Exception {
        sendBodyAndHeader(BEFORE_READ, null, null, null, null);
    }

    @Override
    public void afterRead(final Object item) throws Exception {
        sendBodyAndHeader(AFTER_READ, item, null, null, null);
    }

    @Override
    public void onReadError(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_READ_ERROR, null, null, null, ex);
    }

    // ItemWriteListener
    @Override
    public void beforeWrite(final List<Object> items) throws Exception {
        sendBodyAndHeader(BEFORE_WRITE, null, null, items, null);
    }

    @Override
    public void afterWrite(final List<Object> items) throws Exception {
        sendBodyAndHeader(AFTER_WRITE, null, null, items, null);
    }

    @Override
    public void onWriteError(final List<Object> items, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_WRITE_ERROR, null, null, items, ex);
    }

    // RetryProcessListener
    @Override
    public void onRetryProcessException(final Object item, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_RETRY_PROCESS_EXCEPTION, item, null, null, ex);
    }

    // RetryReadListener
    @Override
    public void onRetryReadException(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_RETRY_READ_EXCEPTION, null, null, null, ex);
    }

    // RetryWriteListener
    @Override
    public void onRetryWriteException(final List<Object> items, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_RETRY_WRITE_EXCEPTION, null, null, items, ex);
    }

    // SkipProcessListener
    @Override
    public void onSkipProcessItem(final Object item, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_SKIP_PROCESS_ITEM, item, null, null, ex);
    }

    // SkipReadListener
    @Override
    public void onSkipReadItem(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_SKIP_READ_ITEM, null, null, null, ex);
    }

    // SkipWriteListener
    @Override
    public void onSkipWriteItem(final List<Object> items, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_SKIP_WRITE_ITEM, null, null, items, ex);
    }


    /**
     * Sends the step execution event message to the configured Camel endpoint.
     * The message has the current {@code StepExecution} as the body, and
     * a header to indicate the event type.
     *
     * @param eventType
     */
    protected void sendBodyAndHeader(final String eventType,
                                     final Object item,
                                     final Object result,
                                     final List<Object> items,
                                     final Exception exception) {
        final ChunkExecutionInfo chunkExecutionInfo =
                new ChunkExecutionInfo(jobContext.getExecutionId(), jobContext.getJobName(),
                        stepContext.getStepExecutionId(), stepContext.getStepName(),
                        item, result, items, exception);

        producerTemplate.sendBodyAndHeader(endpoint, chunkExecutionInfo, EventType.EVENT_TYPE, eventType);
    }

}
