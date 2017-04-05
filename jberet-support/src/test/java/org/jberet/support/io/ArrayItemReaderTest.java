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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A test class that reads from inlined array data.
 */
public final class ArrayItemReaderTest {
    static final String arrayItemReaderjobName = "org.jberet.support.io.ArrayItemReaderTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    public static List<Object> items;

    @Test
    public void stringArrayDefaultBeanType() throws Exception {
        final String arrayContent = "[\"x\", \"y\", \"z\"]";
        test0(arrayContent, null, 3);
    }

    @Test
    public void stringArray() throws Exception {
        final String arrayContent = "[\"1\", \"2\", \"3\"]";
        test0(arrayContent, String.class, 3);
    }

    @Test
    public void IntegerArray() throws Exception {
        final String arrayContent = "[1, 2, 3]";
        test0(arrayContent, Integer.class, 3);
    }

    @Test
    public void LongArray() throws Exception {
        final String arrayContent = "[1]";
        test0(arrayContent, Long.class, 1);
    }

    @Test
    public void CharacterArray() throws Exception {
        final String arrayContent = "[\"!\", \"@\", \"#\"]";
        test0(arrayContent, Character.class, 3);
    }

    @Test
    public void ByteArray() throws Exception {
        final String arrayContent = "[1, 2, 3]";
        test0(arrayContent, Byte.class, 3);
    }

    @Test
    public void MovieArray() throws Exception {
        final String arrayContent = "[\n" +
        "{\"rank\" : 1, \"tit\" : \"Number One\", \"grs\" : 1000, \"opn\" : \"2017-01-01\"},\n" +
        "{\"rank\" : 2, \"tit\" : \"Number Two\", \"grs\" : 2000, \"opn\" : \"2017-02-02\"},\n" +
        "{\"rank\" : 3, \"tit\" : \"Number Three\", \"grs\" : 3000, \"opn\" : \"2017-03-03\"},\n" +
        "{\"rank\" : 4, \"tit\" : \"Number Four\", \"grs\" : 4000, \"opn\" : \"2017-04-04\"},\n" +
        "{\"rank\" : 5, \"tit\" : \"Number Five\", \"grs\" : 5000, \"opn\" : \"2017-05-05\"}\n" +
        "]";

        test0(arrayContent, Movie.class, 5);
    }

    /**
     * Same as {@link #MovieArray()}, except that this test reads batch data from a file resource,
     * instead of inlined batch data.
     *
     * @throws Exception upon errors
     */
    @Test
    public void fileResource() throws Exception {
        final String resourceFile = "movies-2012.json";
        test0(resourceFile, Movie.class, 100);
    }


    private void test0(String resource, Class<?> beanType, int expectedSize) throws Exception {
        final Properties params = new Properties();
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);

        if (beanType != null) {
            params.setProperty(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        }

        final long jobExecutionId = jobOperator.start(arrayItemReaderjobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(expectedSize, items.size());
        assertEquals((beanType == null ? String.class : beanType), items.get(0).getClass());

        if (items != null) {
            items.clear();
        }
    }

}
