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
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public final class ExcelWriterTest {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String jobName = "org.jberet.support.io.ExcelWriterTest";
    static final String moviesSheetName = "Movies 2012";

    @Test
    public void testMoviesBeanTypeFull() throws Exception {
        testReadWrite0(JsonItemReaderTest.movieJson, "testMoviesBeanTypeFull.xlsx",
                null, null, MovieTest.header,
                Movie.class, moviesSheetName);
    }

    private void testReadWrite0(final String resource, final String writeResource,
                                final String start, final String end, final String header,
                                final Class<?> beanType, final String sheetName) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        final File writeResourceFile = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
        params.setProperty("writeResource", writeResourceFile.getPath());
        params.setProperty("resource", resource);

        if (header != null) {
            params.setProperty("header", header);
        }
        if (sheetName != null) {
            params.setProperty("sheetName", sheetName);
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
    }
}
