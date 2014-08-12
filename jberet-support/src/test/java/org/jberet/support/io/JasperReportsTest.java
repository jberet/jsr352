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

public class JasperReportsTest {
    private static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String testJobName = "org.jberet.support.io.JasperReportsTest.xml";
    static final String moviesTemplate = "movies.jasper";
    static final String moviesCsvCharset = null;

    @Test
    public void csvToPdfFile() throws Exception {
        testJasperReports(testJobName, moviesTemplate, MovieTest.moviesCsv, moviesCsvCharset,
                "pdf", "csvToPdfFile.pdf", null);
    }

    @Test
    public void csvToDefaultFileType() throws Exception {
        testJasperReports(testJobName, moviesTemplate, MovieTest.moviesCsv, moviesCsvCharset,
                null, "csvToDefaultFileType.pdf", null);
    }

    @Test
    public void csvToHtmlFile() throws Exception {
        testJasperReports(testJobName, moviesTemplate, MovieTest.moviesCsv, moviesCsvCharset,
                "html", "csvToHtmlFile.html", null);
    }

    @Test
    public void csvToJrprintFile() throws Exception {
        testJasperReports(testJobName, moviesTemplate, MovieTest.moviesCsv, moviesCsvCharset,
                "jrprint", "csvToJrprintFile.jrprint", null);
    }

    void testJasperReports(final String jobName, final String template, final String resource, final String charset,
                           final String outputType, final String outputFile, final String reportParameters) throws Exception {
        final Properties params = new Properties();
        final File outputAsFile;
        if (outputFile != null) {
            outputAsFile = new File(CsvItemReaderWriterTest.tmpdir, outputFile);
            params.setProperty("outputFile", outputAsFile.getPath());
        }
        if (charset != null) {
            params.setProperty("charset", charset);
        }
        if (resource != null) {
            params.setProperty("resource", resource);
        }
        if (template != null) {
            params.setProperty("template", template);
        }
        if (outputType != null) {
            params.setProperty("outputType", outputType);
        }
        if (reportParameters != null) {
            params.setProperty("reportParameters", reportParameters);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

}
