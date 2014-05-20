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

    static final String moviesTemplateResource = "person-movies.xltx";
    static final String moviesTemplateSheetName = "Movies";
    static final String moviesTemplateHeaderRow = "0";

    @Test
    public void testMoviesBeanTypeFull() throws Exception {
        testReadWrite0(JsonItemReaderTest.movieJson, "testMoviesBeanTypeFull.xlsx", MovieTest.header,
                null, null, null,
                Movie.class, moviesSheetName);
    }

    //verifies an existing excel file can be used as a template for populating data into a new excel file.
    //the template contains format (set font color to blue) that should be applied to the generated output excel file.
    //the header is also configured in template file, so no need to explicitly specify header property in job.xml.
    @Test
    public void testMoviesBeanTypeFullTemplate() throws Exception {
        testReadWrite0(JsonItemReaderTest.movieJson, "testMoviesBeanTypeFullTemplate.xlsx", null,
                moviesTemplateResource, moviesTemplateSheetName, moviesTemplateHeaderRow,
                Movie.class, moviesSheetName);
    }

    //similar to the above test, but passing in external header
    @Test
    public void testMoviesBeanTypeFullTemplateHeader() throws Exception {
        testReadWrite0(JsonItemReaderTest.movieJson, "testMoviesBeanTypeFullTemplateHeader.xlsx", MovieTest.header,
                moviesTemplateResource, moviesTemplateSheetName, null,
                Movie.class, moviesSheetName);
    }

    private void testReadWrite0(final String resource, final String writeResource, final String header,
                                final String templateResource, final String templateSheetName, final String templateHeaderRow,
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

        if (templateResource != null) {
            params.setProperty("templateResource", templateResource);
        }
        if (templateSheetName != null) {
            params.setProperty("templateSheetName", templateSheetName);
        }
        if (templateHeaderRow != null) {
            params.setProperty("templateHeaderRow", templateHeaderRow);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
