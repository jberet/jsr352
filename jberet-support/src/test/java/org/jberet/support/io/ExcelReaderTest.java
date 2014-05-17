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

public final class ExcelReaderTest {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    static final String jobName = "org.jberet.support.io.ExcelReaderTest";
    static final String personMoviesResource = "person-movies.xlsx";
    static final String moviesSheetName = "Sheet2";
    static final String personSheetName = "Sheet1";
    static final String capeResource = "ie_data.xls";
    static final String capeSheetName = "Data";
    static final String capeHeader =
            "date, sp, dividend, earnings, cpi, dateFraction, longInterestRate, realPrice, realDividend, realEarnings, cape";
    static final String capeFullExpected = "1871.01, 1871.02, 1871.03, 1950.01, 1950.02, 1950.03, 2014.01, 2014.02, 2014.03, 2014.04";

    @Test
    public void testMoviesBeanTypeFull() throws Exception {
        testReadWrite0(personMoviesResource, "testMoviesBeanTypeFull.out",
                "1", null, null,
                Movie.class, moviesSheetName, "0",
                MovieTest.expectFull, null);
    }

    @Test
    public void testMoviesMapTypeFull() throws Exception {
        testReadWrite0(personMoviesResource, "testMoviesMapTypeFull.out",
                "1", null, null,
                Map.class, moviesSheetName, "0",
                MovieTest.expectFull, null);
    }

    @Test
    public void testMoviesBeanType2_4() throws Exception {
        testReadWrite0(personMoviesResource, "testMoviesBeanType2_4.out",
                "2", "4", null,
                Movie.class, moviesSheetName, "0",
                MovieTest.expect2_4, MovieTest.forbid2_4);
    }

    @Test
    public void testMoviesMapType2_4() throws Exception {
        testReadWrite0(personMoviesResource, "testMoviesMapType2_4.out",
                "2", "4", null,
                Map.class, moviesSheetName, "0",
                MovieTest.expect2_4, MovieTest.forbid2_4);
    }

    @Test
    public void testMoviesBeanType1_2() throws Exception {
        testReadWrite0(personMoviesResource, "testMoviesBeanType1_2.out",
                "1", "2", null,
                Movie.class, moviesSheetName, "0",
                MovieTest.expect1_2, MovieTest.forbid1_2);
    }

    @Test
    public void testMoviesMapType1_2() throws Exception {
        testReadWrite0(personMoviesResource, "testMoviesMapType1_2.out",
                "1", "2", null,
                Map.class, moviesSheetName, "0",
                MovieTest.expect1_2, MovieTest.forbid1_2);
    }


    @Test
    public void testPersonBeanType1_5() throws Exception {
        testReadWrite0(personMoviesResource, "testPersonBeanType1_5.out",
                "1", "5", null,
                Person.class, personSheetName, "0",
                CsvItemReaderWriterTest.personResourceExpect1_5, CsvItemReaderWriterTest.personResourceForbid);
    }

    //verify .xls excel format, use fieldMapping in lieu of header, handling of formula cells
    @Test
    public void testCapeBeanTypeFull() throws Exception {
        testReadWrite0(capeResource, "testCapeBeanTypeFull.out",
                "8", "1727", capeHeader,
                Cape.class, capeSheetName, null,
                capeFullExpected, null);
    }

    private void testReadWrite0(final String resource, final String writeResource,
                                final String start, final String end, final String header,
                                final Class<?> beanType, final String sheetName, final String headerRow,
                                final String expect, final String forbid) throws Exception {
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
