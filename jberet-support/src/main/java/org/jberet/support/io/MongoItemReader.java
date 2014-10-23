/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from a collection in a MongoDB database.
 *
 * @see     MongoItemWriter
 * @see     MongoItemReaderWriterBase
 * @since   1.0.2
 */
@Named
@Dependent
public class MongoItemReader extends MongoItemReaderWriterBase implements ItemReader {
    /**
     * Query criteria or conditions, which identify the documents that MongoDB returns to the client.
     * Its value is a JSON string. Optional property and defaults to null. For example,
     * <pre>
     *  { age: { $gt: 18 } }
     * </pre>
     */
    @Inject
    @BatchProperty
    protected String criteria;

    /**
     * Specifies the fields from the matching documents to return. Its value is a JSON string. Optional property and
     * defaults to null. For example,
     * <pre>
     *     { name: 1, address: 1}
     * </pre>
     */
    @Inject
    @BatchProperty
    protected String projection;

    /**
     * Modifies the query to limit the number of matching documents to return to the client. Optional property and
     * defaults to null (limit is not set).
     */
    @Inject
    @BatchProperty
    protected int limit;

    /**
     * Limits the number of elements returned in one batch. A cursor typically fetches a batch of result objects and
     * store them locally. If batchSize is positive, it represents the size of each batch of objects retrieved.
     * It can be adjusted to optimize performance and limit data transfer. If batchSize is negative,
     * it will limit of number objects returned, that fit within the max batch size limit (usually 4MB),
     * and cursor will be closed. For example if batchSize is -10, then the server will return a maximum of 10
     * documents and as many as can fit in 4MB, then close the cursor.
     * <p>
     * Note that this feature is different from limit() in that documents must fit within a maximum size,
     * and it removes the need to send a request to close the cursor server-side. The batch size can be changed
     * even after a cursor is iterated, in which case the setting will apply on the next batch retrieval.
     *
     * @see "com.mongodb.DBCursor#batchSize(int)"
     */
    @Inject
    @BatchProperty
    protected int batchSize;

    /**
     * Specifies how to sort the cursor's elements. Optional property and defaults to null. Its value is a JSON string,
     * for example,
     * <pre>
     *     { age: 1 }
     * </pre>
     * @see "com.mongodb.DBCursor#sort(com.mongodb.DBObject)"
     */
    @Inject
    @BatchProperty
    protected String sort;

    /**
     * Specifies the number of elements to discard at the beginning of the cursor. Optional property and defaults to 0
     * (do not discard any elements).
     *
     * @see "com.mongodb.DBCursor#skip(int)"
     */
    @Inject
    @BatchProperty
    protected int skip;

    protected org.mongojack.DBCursor<Object> cursor;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.init();
        final DBObject query = criteria == null ? new BasicDBObject() : (DBObject) JSON.parse(criteria);
        cursor = projection == null ? jacksonCollection.find(query) : jacksonCollection.find(query, (DBObject) JSON.parse(projection));

        if (limit != 0) {
            cursor.limit(limit);
        }
        if (sort != null) {
            cursor.sort((DBObject) JSON.parse(sort));
        }
        if (checkpoint != null) {
            cursor.skip((Integer) checkpoint);
        } else if (skip > 0) {
            cursor.skip(skip);
        }
        if (batchSize != 0) {
            cursor.batchSize(batchSize);
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (cursor.hasNext()) {
            final Object readValue = cursor.next();
            if (!skipBeanValidation) {
                ItemReaderWriterBase.validate(readValue);
            }
            return readValue;
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return cursor.numSeen() - 1;
    }
}
