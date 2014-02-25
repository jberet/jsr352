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

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.jberet.support._private.SupportLogger;
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
        if (uri != null) {
            mongoClient = (MongoClient) Mongo.Holder.singleton().connect(new MongoClientURI(uri));
        } else {
            if (host == null) {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "host");
            }
            if (beanType == null) {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "beanType");
            }
            if (database == null) {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "database");
            }
            if (collection == null) {
                throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "collection");
            }

            //The format of the URI is:
            //mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
            final StringBuilder uriVal = new StringBuilder("mongodb://");
            if (user != null) {
                uriVal.append(user).append(':').append(password == null ? "" : password).append('@');
            }
            uriVal.append(host).append('/').append(database);

            if (options != null && !options.isEmpty()) {
                uriVal.append('?').append(options);
            }
            mongoClient = (MongoClient) Mongo.Holder.singleton().connect(new MongoClientURI(uriVal.toString()));
        }
        db = mongoClient.getDB(database);
        jacksonCollection = JacksonDBCollection.wrap(db.getCollection(collection), beanType, String.class);
    }
}
