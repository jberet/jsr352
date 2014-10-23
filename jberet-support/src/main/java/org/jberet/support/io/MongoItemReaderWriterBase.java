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
 *
 * @see     MongoItemReader
 * @see     MongoItemWriter
 * @since   1.0.2
 */
public abstract class MongoItemReaderWriterBase extends ItemReaderWriterBase {
    /**
     * For {@link MongoItemReader}, it's the java type that each data item should be converted to; for
     * {@link MongoItemWriter}, it's the java type for each incoming data item. Required property, and valid values are
     * any data-representing bean class, for example,
     * <p>
     * <ul>
     * <li>{@code org.jberet.support.io.StockTrade}</li>
     * <li>{@code org.jberet.support.io.Person}</li>
     * <li>{@code my.own.custom.ItemBean}</li>
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * JNDI lookup name for {@code com.mongodb.MongoClient}. Optional property and defaults to null. When this property
     * is specified, its value will be used to look up an instance of {@code com.mongodb.MongoClient}, which is
     * typically created and administrated externally (e.g., inside application server). Otherwise, a new instance of
     * {@code com.mongodb.MongoClient} will be created instead of lookup.
     *
     * @see MongoClientObjectFactory
     * @see "com.mongodb.MongoClient"
     */
    @Inject
    @BatchProperty
    protected String mongoClientLookup;

    /**
     * The Mongo client URI. See {@code com.mongodb.MongoClientURI} docs for syntax and details. Optional property.
     * When this property is present, it should encompass all information necessary to establish a client connection.
     * When this property is not present, other properties (e.g., {@link #host}, {@link #database}, {@link #user},
     * {@link #password}, {@link #options}, and {@link #collection}) should be specified to satisfy
     * {@code com.mongodb.MongoClientURI} requirement.
     *
     * @see "com.mongodb.MongoClientURI"
     */
    @Inject
    @BatchProperty
    protected String uri;

    /**
     * Host and optional port information for creating {@code com.mongodb.MongoClientURI}.  It can be single host and
     * port, or multiple host and port specification in the format
     * {@code host1[:port1][,host2[:port2],...[,hostN[:portN]]] }
     *
     * @see "com.mongodb.MongoClientURI"
     * @see MongoClientObjectFactory#createMongoClientURI(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Inject
    @BatchProperty
    protected String host;

    /**
     * MongoDB database name, e.g., {@code testData}. Optional property and defaults to null.
     *
     * @see "com.mongodb.MongoClientURI"
     */
    @Inject
    @BatchProperty
    protected String database;

    /**
     * MongoDB username. Optional property and defaults to null.
     *
     * @see "com.mongodb.MongoClientURI"
     */
    @Inject
    @BatchProperty
    protected String user;

    /**
     * MongoDB password. Optional property and defaults to null.
     *
     * @see "com.mongodb.MongoClientURI"
     */
    @Inject
    @BatchProperty
    protected String password;

    /**
     * MongoDB client options, e.g., {@code safe=true&wtimeout=1000}. Optional property and defaults to null.
     *
     * @see "com.mongodb.MongoClientURI"
     */
    @Inject
    @BatchProperty
    protected String options;

    /**
     * MongoDB collection name, e.g., {@code movies}. Optional property and defaults to null.
     *
     * @see "com.mongodb.MongoClientURI"
     */
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
