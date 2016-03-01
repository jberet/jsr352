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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.WebTarget;

import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code ItemReader} that reads data items from REST resource.
 * <p>
 * Usage example:
 * <pre>
 * &lt;chunk&gt;
 *   &lt;reader ref="restItemReader"&gt;
 *     &lt;properties&gt;
 *       &lt;property name="restUrl" value="http://localhost:8080/appName/rest-api/movies"/&gt;
 *
 *       &lt;!-- starts from item 3 for the initial reading, skipping the first 3 elements (0, 1, 2) --&gt;
 *       &lt;!-- if offset not set, will start reading from the beginning --&gt;
 *       &lt;property name="offset" value="3"/&gt;
 *
 *       &lt;!-- configure each REST call to return a maximum 20 items --&gt;
 *       &lt;property name="limit" value="20"/&gt;
 *
 *       &lt;!-- type of each element in REST response entity --&gt;
 *       &lt;property name="beanType" value="org.jberet.samples.wildfly.common.Movie"/&gt;
 *     &lt;/properties&gt;
 *   &lt;/reader&gt;
 *   ...
 * &lt;chunk&gt;
 * </pre>
 *
 * @see RestItemWriter
 * @see RestItemReaderWriterBase
 *
 * @since 1.3.0
 */
@Named
@Dependent
public class RestItemReader extends RestItemReaderWriterBase implements ItemReader {
    /**
     * Default key for offset query parameter.
     */
    public static final String DEFAULT_OFFSET_KEY = "offset";

    /**
     * Default value for offset query parameter.
     */
    public static final String DEFAULT_OFFSET = "0";

    /**
     * Default key for limit query parameter.
     */
    public static final String DEFAULT_LIMIT_KEY = "limit";

    /**
     * Default value for limit query parameter.
     */
    public static final String DEFAULT_LIMIT = "10";

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
     * The value of the {@code offset} property, which specifies the starting
     * point for reading. If not specified, it defaults to {@value #DEFAULT_OFFSET}.
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

    /**
     * The value of the {@code limit} property, which specifies the maximum
     * number of items to read. If not specified, it defaults to {@value #DEFAULT_LIMIT}.
     */
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
        super.open(checkpoint);
        if(httpMethod == null) {
            httpMethod = HttpMethod.GET;
        } else {
            httpMethod = httpMethod.toUpperCase(Locale.ENGLISH);
            if (!HttpMethod.GET.equals(httpMethod) && !HttpMethod.DELETE.equals(httpMethod)) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, httpMethod, "httpMethod");
            }
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

        final Object[] recordsArray = HttpMethod.GET.equals(httpMethod) ?
                (Object[]) target.request().get(entityType) :
                (Object[]) target.request().delete(entityType);
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
        super.close();
        recordsBuffer.clear();
    }
}
