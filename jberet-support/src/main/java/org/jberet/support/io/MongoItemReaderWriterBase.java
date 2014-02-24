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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
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
    protected String[] host;

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
    protected String collection;

    protected MongoClient mongoClient;
    protected DB db;
    protected JacksonDBCollection<Object, String> jacksonCollection;

    protected void init() throws Exception {
        if (host == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "host");
        }
        if (beanType == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "beanType");
        }
        MongoCredential credential = null;
        if (user != null) {
            credential = MongoCredential.createMongoCRCredential(user, database, password.toCharArray());
        }
        if (host.length == 1) {
            mongoClient = credential == null ? new MongoClient(new ServerAddress(host[0])) :
                    new MongoClient(new ServerAddress(host[0]), Arrays.asList(credential));
        } else {
            final List<ServerAddress> serverAddresses = new ArrayList<ServerAddress>();
            for (final String s : host) {
                serverAddresses.add(new ServerAddress(s));
            }
            mongoClient = credential == null ? new MongoClient(serverAddresses) :
                    new MongoClient(serverAddresses, Arrays.asList(credential));
        }
        db = mongoClient.getDB(database);
        jacksonCollection = JacksonDBCollection.wrap(db.getCollection(collection), beanType, String.class);
    }
}
