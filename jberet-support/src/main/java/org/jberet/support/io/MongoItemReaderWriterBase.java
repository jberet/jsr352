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

import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.naming.InitialContext;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.jberet.support._private.SupportMessages;
import org.mongojack.JacksonDBCollection;

/**
 * Base class for {@link org.jberet.support.io.MongoItemReader} and {@link org.jberet.support.io.MongoItemWriter}.
 * their common batch artifact properties are declared and injected here.
 */
public abstract class MongoItemReaderWriterBase {
    @Inject
    @BatchProperty
    protected Class beanType;

    @Inject
    @BatchProperty
    protected String mongoClientLookup;

    @Inject
    @BatchProperty
    protected String uri;

    @Inject
    @BatchProperty
    protected String host;

    @Inject
    @BatchProperty
    protected String database;

    @Inject
    @BatchProperty
    protected String user;

    @Inject
    @BatchProperty
    protected String password;

    @Inject
    @BatchProperty
    protected String options;

    @Inject
    @BatchProperty
    protected String collection;

    protected MongoClient mongoClient;
    protected DB db;
    protected JacksonDBCollection<Object, String> jacksonCollection;

    protected void init() throws Exception {
        if (beanType == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "beanType");
        }
        if (mongoClientLookup == null) {
            final MongoClientURI clientURI;
            if (uri != null) {
                clientURI = new MongoClientURI(uri);
                if (database == null) {
                    database = clientURI.getDatabase();
                }
                if (collection == null) {
                    collection = clientURI.getCollection();
                }
            } else {
                clientURI = MongoClientObjectFactory.createMongoClientURI(host, database, collection, options, user, password);
            }
            mongoClient = (MongoClient) Mongo.Holder.singleton().connect(clientURI);
        } else {
            mongoClient = InitialContext.doLookup(mongoClientLookup);
        }

        db = mongoClient.getDB(database);
        jacksonCollection = JacksonDBCollection.wrap(db.getCollection(collection), beanType, String.class);
    }
}
