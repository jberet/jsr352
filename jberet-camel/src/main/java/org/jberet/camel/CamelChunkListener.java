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

/**
 * An implementation of batch chunk listeners that sends chunk execution events
 * to the configured Camel endpoint. The following are the chunk listener
 * interfaces implemented by this class, and supported types of chunk
 * execution events:
 * <ul>
 *     <li>{@code javax.batch.api.chunk.listener.ChunkListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#BEFORE_CHUNK}
 *              <li>{@value org.jberet.camel.EventType#ON_CHUNK_ERROR}
 *              <li>{@value org.jberet.camel.EventType#AFTER_CHUNK}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.ItemProcessListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#BEFORE_PROCESS}
 *              <li>{@value org.jberet.camel.EventType#AFTER_PROCESS}
 *              <li>{@value org.jberet.camel.EventType#ON_PROCESS_ERROR}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.ItemReadListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#BEFORE_READ}
 *              <li>{@value org.jberet.camel.EventType#AFTER_READ}
 *              <li>{@value org.jberet.camel.EventType#ON_READ_ERROR}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.ItemWriteListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#BEFORE_WRITE}
 *              <li>{@value org.jberet.camel.EventType#AFTER_WRITE}
 *              <li>{@value org.jberet.camel.EventType#ON_WRITE_ERROR}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.RetryProcessListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#ON_RETRY_PROCESS_EXCEPTION}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.RetryReadListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#ON_RETRY_READ_EXCEPTION}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.RetryWriteListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#ON_RETRY_WRITE_EXCEPTION}
 *
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.SkipProcessListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#ON_SKIP_PROCESS_ITEM}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.SkipReadListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#ON_SKIP_READ_ITEM}
 *          </ul>
 *     <li>{@code javax.batch.api.chunk.listener.SkipWriteListener}
 *          <ul>
 *              <li>{@value org.jberet.camel.EventType#ON_SKIP_WRITE_ITEM}
 *          </ul>
 * </ul>
 * The body of the message sent is the current {@link ChunkExecutionInfo}.
 * Each message also contains a header to indicate the event type:
 * its key is {@value org.jberet.camel.EventType#KEY}, and value is
 * one from the above list.
 * <p>
 * The target Camel endpoint is configured through batch property
 * {@code endpoint} in job XML. For example,
 * <pre>
 * &lt;job id="camelChunkListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0"&gt;
 *   &lt;step id="camelChunkListenerTest.step1"&gt;
 *     &lt;listeners&gt;
 *       &lt;listener ref="camelChunkListener"&gt;
 *         &lt;properties&gt;
 *           &lt;property name="endpoint" value="#{jobParameters['endpoint']}"/&gt;
 *         &lt;/properties&gt;
 *       &lt;/listener&gt;
 *     &lt;/listeners&gt;
 *     ... ...
 * </pre>
 *
 * @see CamelJobListener
 * @see CamelStepListener
 * @since 1.3.0
 */
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

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#BEFORE_CHUNK}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#BEFORE_CHUNK}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo}.
     *
     * @throws Exception
     */
    @Override
    public void beforeChunk() throws Exception {
        sendBodyAndHeader(BEFORE_CHUNK, null, null, null, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_CHUNK_ERROR}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_CHUNK_ERROR}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo}, which
     * contains the exception along with other chunk execution info.
     *
     * @throws Exception
     */

    @Override
    public void onError(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_CHUNK_ERROR, null, null, null, ex);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#AFTER_CHUNK}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#AFTER_CHUNK}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo}.
     *
     * @throws Exception
     */

    @Override
    public void afterChunk() throws Exception {
        sendBodyAndHeader(AFTER_CHUNK, null, null, null, null);
    }

    // ItemProcessListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#BEFORE_PROCESS}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#BEFORE_PROCESS}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code item} being processed, along with other
     * chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void beforeProcess(final Object item) throws Exception {
        sendBodyAndHeader(BEFORE_PROCESS, item, null, null, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#AFTER_PROCESS}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#AFTER_PROCESS}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code item} being processed and {@code result},
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void afterProcess(final Object item, final Object result) throws Exception {
        sendBodyAndHeader(AFTER_PROCESS, item, result, null, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_PROCESS_ERROR}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_PROCESS_ERROR}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code item} being processed and the exception,
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onProcessError(final Object item, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_PROCESS_ERROR, item, null, null, ex);
    }

    // ItemReadListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#BEFORE_READ}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#BEFORE_READ}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo}.
     *
     * @throws Exception
     */
    @Override
    public void beforeRead() throws Exception {
        sendBodyAndHeader(BEFORE_READ, null, null, null, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#AFTER_READ}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#AFTER_READ}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code item} being read, along with other
     * chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void afterRead(final Object item) throws Exception {
        sendBodyAndHeader(AFTER_READ, item, null, null, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_READ_ERROR}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_READ_ERROR}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the exception, along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onReadError(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_READ_ERROR, null, null, null, ex);
    }

    // ItemWriteListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#BEFORE_WRITE}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#BEFORE_WRITE}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code items} being written, along with other
     * chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void beforeWrite(final List<Object> items) throws Exception {
        sendBodyAndHeader(BEFORE_WRITE, null, null, items, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#AFTER_WRITE}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#AFTER_WRITE}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code items} being written, along with other
     * chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void afterWrite(final List<Object> items) throws Exception {
        sendBodyAndHeader(AFTER_WRITE, null, null, items, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_WRITE_ERROR}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_WRITE_ERROR}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code items} being written and the exception,
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onWriteError(final List<Object> items, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_WRITE_ERROR, null, null, items, ex);
    }

    // RetryProcessListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_RETRY_PROCESS_EXCEPTION}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_RETRY_PROCESS_EXCEPTION}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code item} being retry-processed and the cause exception,
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onRetryProcessException(final Object item, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_RETRY_PROCESS_EXCEPTION, item, null, null, ex);
    }

    // RetryReadListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_RETRY_READ_EXCEPTION}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_RETRY_READ_EXCEPTION}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the cause exception, along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onRetryReadException(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_RETRY_READ_EXCEPTION, null, null, null, ex);
    }

    // RetryWriteListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_RETRY_WRITE_EXCEPTION}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_RETRY_WRITE_EXCEPTION}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code items} being retry-written and the cause exception,
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onRetryWriteException(final List<Object> items, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_RETRY_WRITE_EXCEPTION, null, null, items, ex);
    }

    // SkipProcessListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_SKIP_PROCESS_ITEM}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_SKIP_PROCESS_ITEM}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the skipped processing {@code item} and the cause exception,
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onSkipProcessItem(final Object item, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_SKIP_PROCESS_ITEM, item, null, null, ex);
    }

    // SkipReadListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_SKIP_READ_ITEM}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_SKIP_READ_ITEM}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the exception that caused skipping item, along with other
     * chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onSkipReadItem(final Exception ex) throws Exception {
        sendBodyAndHeader(ON_SKIP_READ_ITEM, null, null, null, ex);
    }

    // SkipWriteListener
    /**
     * {@inheritDoc}
     * <p>
     * This method sends {@value org.jberet.camel.EventType#ON_SKIP_WRITE_ITEM}
     * event to the configured Camel endpoint. The message contains a header:
     * <pre>
     *     {@value org.jberet.camel.EventType#KEY}={@value org.jberet.camel.EventType#ON_SKIP_WRITE_ITEM}
     * </pre>
     * , and the message body is the current {@link org.jberet.camel.ChunkExecutionInfo},
     * which contains the current {@code items} being skipped for writing and the cause exception,
     * along with other chunk execution info.
     *
     * @throws Exception
     */
    @Override
    public void onSkipWriteItem(final List<Object> items, final Exception ex) throws Exception {
        sendBodyAndHeader(ON_SKIP_WRITE_ITEM, null, null, items, ex);
    }


    /**
     * Sends the chunk execution event message to the configured Camel endpoint.
     * The message has the current {@link ChunkExecutionInfo} as the body, and
     * a header to indicate the event type.
     *
     * @param eventType event type as defined in {@link EventType}
     * @param item the item being processed, if applicable; may be null
     * @param result the result from processing, if applicable; may be null
     * @param items the items for writing by the item writer, if applicable; may be null
     * @param exception any exception during read, write or processing; may be null
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

        producerTemplate.sendBodyAndHeader(endpoint, chunkExecutionInfo, EventType.KEY, eventType);
    }

}
