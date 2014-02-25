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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.util.JSON;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test class that reads resource from MongoDB and writes to another MongoDB collection.
 */
public final class MongoItemReaderTest {
    static MongoClient mongoClient;
    static DB db;
    static final String jobName = "org.jberet.support.io.MongoItemReaderTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    static final String databaseName = "testData";
    static final String mongoClientUri = "mongodb://localhost/" + databaseName;
    static final String readCollection = "movies";
    static final String writeCollection = "movies.out";

    @BeforeClass
    public static void beforeClass() throws Exception {
        mongoClient = (MongoClient) Mongo.Holder.singleton().connect(new MongoClientURI(mongoClientUri));
        db = mongoClient.getDB(databaseName);
    }

    @Before
    public void before() throws Exception {
        dropCollection(writeCollection);
        addTestData(JsonItemReaderTest.movieJson);
    }

    @Test
    public void testMongoMovieBeanTypeLimit2() throws Exception {
        testReadWrite0(null, "2", 2, MovieTest.expect1_2, MovieTest.forbid1_2);
    }

    @Test
    public void testMongoMovieBeanTypeLimit3Skip1() throws Exception {
        testReadWrite0("1", "3", 3, MovieTest.expect2_4, MovieTest.forbid2_4);
    }

    @Test
    public void testMongoMovieBeanTypeFull() throws Exception {
        testReadWrite0(null, null, 100, MovieTest.expectFull, null);
    }

    private void testReadWrite0(final String skip, final String limit, final int size, final String expect, final String forbid) throws Exception {
        final Properties params = new Properties();
        if (skip != null) {
            params.setProperty("skip", skip);
        }
        if (limit != null) {
            params.setProperty("limit", limit);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        validate(size, expect, forbid);
    }

    static void dropCollection(final String coll) throws Exception {
        final DBCollection collection = db.getCollection(coll);
        collection.drop();
    }

    static void addTestData(final String dataResource) throws Exception {
        final DBCollection collection = db.getCollection(readCollection);
        if (collection.find().count() == 100) {
            System.out.printf("The readCollection %s already contains 100 items, skip adding test data.%n", readCollection);
            return;
        }

        final InputStream inputStream = MongoItemReaderTest.class.getClassLoader().getResourceAsStream(dataResource);
        if (inputStream == null) {
            throw new IllegalStateException("The inputStream for the test data is null");
        }

        final JsonFactory jsonFactory = new MappingJsonFactory();
        final JsonParser parser = jsonFactory.createParser(inputStream);
        final JsonNode arrayNode = parser.readValueAs(ArrayNode.class);

        final Iterator<JsonNode> elements = arrayNode.elements();
        final List<DBObject> dbObjects = new ArrayList<DBObject>();
        while (elements.hasNext()) {
            final DBObject dbObject = (DBObject) JSON.parse(elements.next().toString());
            dbObjects.add(dbObject);
        }
        collection.insert(dbObjects);
    }

    static void validate(final int size, final String expect, final String forbid) {
        final DBCollection collection = db.getCollection(writeCollection);
        final DBCursor cursor = collection.find();

        try {
            Assert.assertEquals(size, cursor.size());
            final List<String> expects = new ArrayList<String>();
            String[] forbids = CellProcessorConfig.EMPTY_STRING_ARRAY;
            if (expect != null && !expect.isEmpty()) {
                Collections.addAll(expects, expect.split(","));
            }
            if (forbid != null && !forbid.isEmpty()) {
                forbids = forbid.split(",");
            }
            if (expects.size() == 0 && forbids.length == 0) {
                return;
            }
            while (cursor.hasNext()) {
                final DBObject next = cursor.next();
                final String stringValue = next.toString();
                for (final String s : forbids) {
                    if (stringValue.contains(s.trim())) {
                        throw new IllegalStateException("Forbidden string found: " + s);
                    }
                }
                for (final Iterator<String> it = expects.iterator(); it.hasNext(); ) {
                    final String s = it.next();
                    if (stringValue.contains(s.trim())) {
                        System.out.printf("Found expected string: %s%n", s);
                        it.remove();
                    }
                }
            }
            if (expects.size() > 0) {
                throw new IllegalStateException("Some expected strings are still not found: " + expects);
            }
        } finally {
            cursor.close();
        }
    }
}
