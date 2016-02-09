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

package org.jberet.support.io;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code ItemReader} that reads data items from REST resource.
 *
 * @since 1.3.0
 */
@Named
@Dependent
public class RestItemReader implements ItemReader {
    public static final String DEFAULT_OFFSET_KEY = "offset";
    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_LIMIT_KEY = "limit";
    public static final String DEFAULT_LIMIT = "10";

    /**
     * The base URI for the REST call. It usually points to a collection resource URI,
     * from which resources may be retrieved via HTTP GET method. The URI may include
     * additional query parameters other than offset (starting position to read) and
     * limit (maximum number of items to return in each response). Query parameter
     * offset and limit are specified by their own batch properties.
     * <p>
     * For example, {@code http://localhost:8080/restReader/api/movies}
     * <p>
     * This is a required batch property.
     *
     * @see #offset
     * @see #limit
     */
    @Inject
    @BatchProperty
    protected URI restUrl;

    /**
     * Configures the key of the query parameter that specifies the starting
     * position to read in the target REST resource. For example, some REST
     * resource may require {@code start} instead of {@code offset} query
     * parameter for the same purpose.
     * <p>
     * This batch property is optional. If not set, the default key
     * {@value #DEFAULT_OFFSET_KEY} is used.
     */
    @Inject
    @BatchProperty
    protected String offsetKey;

    /**
     *
     */
    @Inject
    @BatchProperty
    protected String offset;

    /**
     * Configures the key of the query parameter that specifies the maximum
     * number of items to return in the REST response. For example, some REST
     * resource may require {@code count} instead of {@code limit} query
     * parameter for the same purpose.
     * <p>
     * This batch property is optional. If not set, the default key
     * {@value #DEFAULT_LIMIT_KEY} is used.
     */
    @Inject
    @BatchProperty
    protected String limitKey;

    @Inject
    @BatchProperty
    protected String limit;

    /**
     * The class of individual element of the response message entity. For example,
     * <ul>
     * <li>{@code java.lang.String}
     * <li>{@code org.jberet.samples.wildfly.common.Movie}
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * The class of the REST response message entity, and is a array of collection
     * type whose component type is {@link #beanType}. For example,
     * <ul>
     *     <li>{@code Movie[]}
     *     <li>{@code java.util.List<Movie>}
     *     <li>{@code java.util.Collection<Movie>}
     * </ul>
     */
    protected Class entityType;

    /**
     * REST client {@code javax.ws.rs.client.Client}, which is instantiated
     * in {@link #open(Serializable)} and closed in {@link #close()}.
     */
    protected Client client;

    /**
     * Current reading position in the target resource, and is returned as
     * the current checkpoint in {@link #checkpointInfo()} method.
     */
    protected int readerPosition;

    /**
     * Internal buffer to hold multiple items retrieved as the {@link #readItem()}
     * method only returns 1 item at a time.
     */
    protected List<Object> recordsBuffer = new ArrayList<Object>();

    /**
     * During the reader opening, the REST client is instantiated, and
     * {@code checkpoint}, if any, is used to position the reader properly.
     *
     * @param checkpoint checkpoint info, null for the first invocation in a new job execution
     * @throws Exception if error occurs
     */
    @SuppressWarnings("unchecked")
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        client = ClientBuilder.newClient();

        if (restUrl == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "restUrl");
        }

        if (offsetKey == null) {
            offsetKey = DEFAULT_OFFSET_KEY;
        }
        if (offset == null) {
            offset = DEFAULT_OFFSET;
        }

        if (checkpoint != null) {
            readerPosition = (Integer) checkpoint;
        } else {
            readerPosition = Integer.parseInt(offset) - 1;
        }

        if (limitKey == null) {
            limitKey = DEFAULT_LIMIT_KEY;
        }
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        }

        if (beanType == null) {
            entityType = Object[].class;
        } else {
            entityType = (java.lang.reflect.Array.newInstance(beanType, 0)).getClass();
        }
    }

    /**
     * Returns reader checkpoint info (int number), which is the last successfully read position.
     *
     * @return reader checkpoint info as int
     */
    @Override
    public Serializable checkpointInfo() {
        return readerPosition;
    }

    /**
     * Reads 1 record and return the result object, and updates the current read position.
     * The REST operation retrieves a collection of records, which are cached in this
     * reader class to mimic the read-one-item-at-a-time behavior.
     * Therefore, the REST call is only made when the local cache does not contains any entries.
     * If no more record can be retrieved via the REST call, null is returned.
     *
     * @return the REST response entity object
     * @throws Exception if error occurs
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object readItem() throws Exception {
        final int size = recordsBuffer.size();
        readerPosition++;

        if (size > 0) {
            // take 1 item from the end of the buffer
            // items were added to the buffer in the reverse order, so the end is the oldest item
            return recordsBuffer.remove(size - 1);
        }

        final WebTarget target = client.target(restUrl)
                .queryParam(offsetKey, readerPosition)
                .queryParam(limitKey, limit);

        Object[] recordsArray = (Object[]) target.request().get(entityType);
        if (recordsArray.length == 0) {
            return null;
        }

        // add (n-1) items to the buffer in reverse order, and directly return the first element
        for (int i = recordsArray.length - 1; i > 0; i--) {
            recordsBuffer.add(recordsArray[i]);
        }
        return recordsArray[0];
    }

    /**
     * closes the REST client and sets it to null.
     */
    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
        recordsBuffer.clear();
    }
}
