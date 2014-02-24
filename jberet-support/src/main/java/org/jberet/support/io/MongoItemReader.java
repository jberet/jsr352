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
import javax.inject.Inject;
import javax.inject.Named;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from a collection in a MongoDB database.
 */
@Named
public class MongoItemReader extends MongoItemReaderWriterBase implements ItemReader {
    @Inject
    @BatchProperty
    protected String criteria;

    @Inject
    @BatchProperty
    protected String projection;

    @Inject
    @BatchProperty
    protected int limit;

    @Inject
    @BatchProperty
    protected int batchSize;

    @Inject
    @BatchProperty
    protected String sort;

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
            return cursor.next();
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return cursor.numSeen() - 1;
    }
}
