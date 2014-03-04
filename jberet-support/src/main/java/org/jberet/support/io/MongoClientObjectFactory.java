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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.naming.spi.ObjectFactory} that produces instance of {@code com.mongodb.MongoClient}.
 * This class can be used to create a custom JNDI resource in an application server.
 * See wildfly.home/docs/schema/jboss-as-naming_2_0.xsd for more details.
 */
public final class MongoClientObjectFactory implements ObjectFactory {
    /**
     * Gets an instance of {@code com.mongodb.MongoClient} based on the resource configuration in the application server.
     * The parameter {@code environment} contains MongoDB client connection properties, and accept the following property:
     * <ul>
     * <li>uri: uri to connect to MongoDB instance</li>
     * <li>host: single host and port, or multiple host and port specification in the format
     * host1[:port1][,host2[:port2],...[,hostN[:portN]]]</li>
     * <li>database: MongoDB database name, e.g., testData</li>
     * <li>collection: MongoDB collection name, e.g., movies</li>
     * <li>options: MongoDB client options, e.g., safe=true&wtimeout=1000</li>
     * <li>user: MongoDB username</li>
     * <li>password: MongoDB password</li>
     * </ul>
     * See also <a href="http://api.mongodb.org/java/2.12/com/mongodb/MongoClientURI.html">MongoClientURI javadoc.</a>
     *
     * @param obj         the JNDI name of {@code com.mongodb.MongoClient} resource
     * @param name        always null
     * @param nameCtx     always null
     * @param environment a {@code Hashtable} of configuration properties for {@code com.mongodb.MongoClient}
     * @return an instance of {@code com.mongodb.MongoClient}
     * @throws Exception any exception occurred
     */
    @Override
    public Object getObjectInstance(final Object obj,
                                    final Name name,
                                    final Context nameCtx,
                                    final Hashtable<?, ?> environment) throws Exception {
        final MongoClient mongoClient;
        final MongoClientURI clientURI;
        final String uri = (String) environment.get("uri");
        if (uri != null) {
            clientURI = new MongoClientURI(uri);
        } else {
            clientURI = createMongoClientURI((String) environment.get("host"),
                    (String) environment.get("database"),
                    (String) environment.get("collection"),
                    (String) environment.get("options"),
                    (String) environment.get("user"),
                    (String) environment.get("password"));
        }
        mongoClient = (MongoClient) Mongo.Holder.singleton().connect(clientURI);
        SupportLogger.LOGGER.tracef("getObjectInstance obtained MongoClient %s with uri %s%n", mongoClient, clientURI);
        return mongoClient;
    }

    /**
     * Creates a {@code com.mongodb.MongoClientURI} with connection properties host, database, collection, options, user,
     * and password. Host property is mandatory and others are optional.
     *
     * @param host       single host and port, or multiple host and port specification in the format
     *                   host1[:port1][,host2[:port2],...[,hostN[:portN]]]
     * @param database   MongoDB database name, e.g., testData
     * @param collection MongoDB collection name, e.g., movies
     * @param options    MongoDB client options, e.g., safe=true&wtimeout=1000
     * @param user       MongoDB username
     * @param password   MongoDB password
     * @return {@code com.mongodb.MongoClientURI} created from passed parameters
     * @throws Exception any exception during the creation of {@code MongoClientURI}
     */
    static MongoClientURI createMongoClientURI(final String host,
                                               final String database,
                                               final String collection,
                                               final String options,
                                               final String user,
                                               final String password) throws Exception {
        if (host == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "host");
        }

        //The format of the URI is:
        //mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
        final StringBuilder uriVal = new StringBuilder("mongodb://");
        if (user != null) {
            uriVal.append(user).append(':').append(password == null ? "" : password).append('@');
        }
        uriVal.append(host).append('/');

        if (database != null) {
            uriVal.append(database);
            if (collection != null) {
                uriVal.append('.').append(collection);
            }
        }

        if (options != null) {
            uriVal.append('?').append(options);
        }
        return new MongoClientURI(uriVal.toString());
    }

}
