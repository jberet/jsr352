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

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public final class ExcelReaderWriterTest {
    static final String jobName = "org.jberet.support.io.ExcelReaderWriterTest";
    static final String resource = "person-movies.xlsx";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Test
    public void testBeanTypeFull() throws Exception {
        testReadWrite0("testBeanTypeFull.out", "1", null,
                Movie.class, "Sheet2", "0",
                MovieTest.expectFull, null);
    }

    @Test
    public void testMapTypeFull() throws Exception {
        testReadWrite0("testMapTypeFull.out", "1", null,
                Map.class, "Sheet2", "0",
                MovieTest.expectFull, null);
    }

    @Test
    public void testBeanType2_4() throws Exception {
        testReadWrite0("testBeanType2_4.out", "2", "4",
                Movie.class, "Sheet2", "0",
                MovieTest.expect2_4, MovieTest.forbid2_4);
    }

    @Test
    public void testMapType2_4() throws Exception {
        testReadWrite0("testMapType2_4.out", "2", "4",
                Map.class, "Sheet2", "0",
                MovieTest.expect2_4, MovieTest.forbid2_4);
    }

    @Test
    public void testBeanType1_2() throws Exception {
        testReadWrite0("testBeanType1_2.out", "1", "2",
                Movie.class, "Sheet2", "0",
                MovieTest.expect1_2, MovieTest.forbid1_2);
    }

    @Test
    public void testMapType1_2() throws Exception {
        testReadWrite0("testMapType1_2.out", "1", "2",
                Map.class, "Sheet2", "0",
                MovieTest.expect1_2, MovieTest.forbid1_2);
    }

    private void testReadWrite0(final String writeResource, final String start, final String end,
                                final Class<?> beanType, final String sheetName, final String headerRow,
                                final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        final File writeResourceFile = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
        params.setProperty("writeResource", writeResourceFile.getPath());
        params.setProperty("resource", resource);

        if (sheetName != null) {
            params.setProperty("sheetName", sheetName);
        }
        if (headerRow != null) {
            params.setProperty("headerRow", headerRow);
        }

        if (start != null) {
            params.setProperty(CsvProperties.START_KEY, start);
        }
        if (end != null) {
            params.setProperty(CsvProperties.END_KEY, end);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        CsvItemReaderWriterTest.validate(writeResourceFile, expect, forbid);
    }
}
