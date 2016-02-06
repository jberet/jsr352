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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
    public static final String DEFAULT_OFFSET_VALUE = "0";
    public static final String DEFAULT_LIMIT_KEY = "limit";
    public static final String DEFAULT_LIMIT_VALUE = "10";

    @Inject
    @BatchProperty
    protected URI restUrl;

    @Inject
    @BatchProperty
    protected String offsetKey;

    @BatchProperty
    protected String offsetValue;

    @Inject
    @BatchProperty
    protected String limitKey;

    @Inject
    @BatchProperty
    protected String limitValue;

    /**
     * The class of the reponse message entity.
     * It can be array types, or java collection types. For example,
     * <ul>
     *     <li>{@code [Ljava.lang.String;}
     *     <li>{@code java.util.Collection}
     *     <li>{@code java.util.List}
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class entityType;

    protected boolean isEntityCollection;

    /**
     * REST client {@code javax.ws.rs.client.Client}, which is instantiated
     * in {@link #open(Serializable)} and closed in {@link #close()}.
     */
    protected Client client;

    protected int readerPosition;

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
        if (offsetValue == null) {
            offsetValue = DEFAULT_OFFSET_VALUE;
        }

        if (checkpoint != null) {
            readerPosition = (Integer) checkpoint;
        } else {
            readerPosition = Integer.parseInt(offsetValue) - 1;
        }

        if (limitKey == null) {
            limitKey = DEFAULT_LIMIT_KEY;
        }
        if (limitValue == null) {
            limitValue = DEFAULT_LIMIT_VALUE;
        }

        if (entityType == null) {
            entityType = Object[].class;
        } else if (Collection.class.isAssignableFrom(entityType)) {
            isEntityCollection = true;
        } else if(!entityType.isArray()){
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, entityType.toString(), "entityType");
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
            return removeLastFromBuffer();
        }

        final WebTarget target = client.target(restUrl)
                .queryParam(offsetKey, readerPosition)
                .queryParam(limitKey, limitValue);

        final Object responseRecords = target.request().get(entityType);
        if (isEntityCollection) {
            Collection recordsCollection = (Collection) responseRecords;
            final int recordsCount = recordsCollection.size();
            if (recordsCount == 0) {
                return null;
            }
            if (List.class.isAssignableFrom(recordsCollection.getClass())) {
                final ListIterator lit = ((List) recordsCollection).listIterator(recordsCount);
                while (lit.hasPrevious()) {
                    recordsBuffer.add(lit.previous());
                }
            } else {
                for (final Iterator it = recordsCollection.iterator(); it.hasNext(); ) {
                    recordsBuffer.add(it.next());
                }
            }
            return removeLastFromBuffer();
        } else {
            Object[] recordsArray = (Object[]) responseRecords;
            if (recordsArray.length == 0) {
                return null;
            }

            // add items to the buffer in reverse order
            for (int i = recordsArray.length - 1; i >= 0; i--) {
                recordsBuffer.add(recordsArray[i]);
            }
            return removeLastFromBuffer();
        }
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

    /**
     * Removes the last element from {@link #recordsBuffer}, and increment
     * {@link #readerPosition}.
     * When calling this method, {@link #recordsBuffer} must not be empty.
     *
     * @return the removed element from {@link #recordsBuffer}
     */
    private Object removeLastFromBuffer() {
        return recordsBuffer.remove(recordsBuffer.size() - 1);
    }
}
