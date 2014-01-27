/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CsvItemReaderTest {
    static final String jobName = "org.jberet.support.io.CsvReaderTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final int waitTimeoutMinutes = 0;

    @Test
    public void testBeanType() throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, "org.jberet.support.io.Person");
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
    }

    @Test
    public void testListType() throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, "java.util.List");
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
    }

    @Test
    public void testMapType() throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, "java.util.Map");
        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
    }

    @Test @Ignore("restore it if needed")
    public void testStringsToInts() throws Exception {
        final String[] ss = {"1", "2", "3", "4"};
        int[] ints = CsvItemReader.convertToIntParams(ss, 0, ss.length);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(4, ints.length);
        Assert.assertEquals(1, ints[0]);
        Assert.assertEquals(2, ints[1]);
        Assert.assertEquals(3, ints[2]);
        Assert.assertEquals(4, ints[3]);

        ints = CsvItemReader.convertToIntParams(ss, 1, ss.length - 1);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(3, ints.length);
        Assert.assertEquals(2, ints[0]);
        Assert.assertEquals(3, ints[1]);
        Assert.assertEquals(4, ints[2]);

        ints = CsvItemReader.convertToIntParams(ss, 2, ss.length - 2);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(2, ints.length);
        Assert.assertEquals(3, ints[0]);
        Assert.assertEquals(4, ints[1]);

        ints = CsvItemReader.convertToIntParams(ss, 3, ss.length - 3);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(1, ints.length);
        Assert.assertEquals(4, ints[0]);
    }

    static Properties createParams(final String key, final String val) {
        final Properties params = new Properties();
        if (key != null) {
            params.setProperty(key, val);
        }
        return params;
    }

}
