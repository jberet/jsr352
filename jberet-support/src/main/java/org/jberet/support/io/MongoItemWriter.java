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
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;
import javax.inject.Named;

import com.mongodb.WriteConcern;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes to a collection in a MongoDB database.
 */
@Named
public class MongoItemWriter extends MongoItemReaderWriterBase implements ItemWriter {
    @Inject
    @BatchProperty
    protected String writeConcern;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.init();
        if (writeConcern != null) {
            mongoClient.setWriteConcern(new WriteConcern(writeConcern));
        }
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        jacksonCollection.insert(items);
    }

    @Override
    public void close() throws Exception {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
