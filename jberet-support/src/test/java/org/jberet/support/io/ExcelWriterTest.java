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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

//these tests do not verify expected or forbidden data in the resulting excel files.
//open the generated excel files to manually verify.  For example,
//  cd $TMPDIR
//  open testMoviesBeanTypeFullTemplateHeader.xlsx
public final class ExcelWriterTest {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.ExcelWriterTest";
    static final String moviesSheetName = "Movies 2012";

    static final String templateResource = "person-movies-company.xltx";
    static final String moviesTemplateSheetName = "Movies";
    static final String moviesTemplateHeaderRow = "0";
    static final String moviesTemplateNoHeaderSheetName = "Movies No Header";

    static final String streamingWriterTestJobName = "org.jberet.support.io.ExcelStreamingWriterTest.xml";
    static final String companyListCsv = "companylist.csv";
    static final String companyListTemplateSheetName = "Company List";
    static final String companyListTemplateHeaderRow = "0";
    static final String companyListCsvNameMapping = "symbol, name, lastSale, marketCap, address, ipoYear, sector, industry, summaryQuote, null";
    static final String companyListCsvCellProcessors = "null;" +
            "null;" +
            "StrReplace('n/a', '0'), ParseDouble;" +
            "StrReplace('n/a', '0'), ParseDouble;" +
            "null;" +
            "null;" +
            "null;" +
            "null;" +
            "null;" +
            "null";

    static final String ibmStockTradeCsv = "IBM_unadjusted.txt";
    static final String ibmStockTradeSheetName = "IBM Stock Minute Date (1998 - 2014)";
    static final String ibmStockTradeNameMapping = "date,time,open,high,low,close,volume";
    static final String ibmStockTradeHeader = "Date,Time,Open,High,Low,Close,Volume";
    static final String ibmStockTradeCellProcessors = "ParseDate('MM/dd/yyyy');" +
            "null; ParseDouble; ParseDouble; ParseDouble; ParseDouble; ParseDouble";

    private String csvNameMapping;
    private String csvCellProcessors;
    private String csvHeaderless;

    @After
    public void after() {
        this.csvCellProcessors = null;
        this.csvNameMapping = null;
        this.csvHeaderless = null;
    }

    @Test
    public void testMoviesBeanTypeFull() throws Exception {
        testReadWrite0(writerTestJobName, JsonItemReaderTest.movieJson, "testMoviesBeanTypeFull.xlsx", MovieTest.header,
                null, null, null,
                Movie.class, moviesSheetName);
    }

    @Test
    public void testMoviesBeanTypeFullStreaming() throws Exception {
        this.csvCellProcessors = MovieTest.cellProcessors;
        testReadWrite0(streamingWriterTestJobName, MovieTest.moviesCsv, "testMoviesBeanTypeFullStreaming.xlsx", MovieTest.header,
                null, null, null,
                Movie.class, moviesSheetName);
    }


    //verifies an existing excel file can be used as a template for populating data into a new excel file.
    //the template contains format (set font color to blue) that should be applied to the generated output excel file.
    //the header is also configured in template file, so no need to explicitly specify header property in job.xml.
    @Test
    public void testMoviesBeanTypeFullTemplate() throws Exception {
        testReadWrite0(writerTestJobName, JsonItemReaderTest.movieJson, "testMoviesBeanTypeFullTemplate.xlsx", null,
                templateResource, moviesTemplateSheetName, moviesTemplateHeaderRow,
                Movie.class, moviesSheetName);
    }

    @Test
    public void testMoviesBeanTypeFullTemplateStreaming() throws Exception {
        this.csvCellProcessors = MovieTest.cellProcessors;
        testReadWrite0(streamingWriterTestJobName, MovieTest.moviesCsv, "testMoviesBeanTypeFullTemplateStreaming.xlsx", null,
                templateResource, moviesTemplateSheetName, moviesTemplateHeaderRow,
                Movie.class, moviesSheetName);
    }


    //similar to the above test, but passing in external header
    //the template sheet has no header, and font color for all rows is configured magenta
    @Test
    public void testMoviesBeanTypeFullTemplateHeader() throws Exception {
        testReadWrite0(writerTestJobName, JsonItemReaderTest.movieJson, "testMoviesBeanTypeFullTemplateHeader.xlsx", MovieTest.header,
                templateResource, moviesTemplateNoHeaderSheetName, null,
                Movie.class, moviesSheetName);
    }

    @Test
    public void testMoviesBeanTypeFullTemplateHeaderStreaming() throws Exception {
        this.csvCellProcessors = MovieTest.cellProcessors;
        testReadWrite0(streamingWriterTestJobName, MovieTest.moviesCsv, "testMoviesBeanTypeFullTemplateHeaderStreaming.xlsx", MovieTest.header,
                templateResource, moviesTemplateNoHeaderSheetName, null,
                Movie.class, moviesSheetName);
    }


    //verifies reading large csv data set and writing excel with ExcelStreamingItemWriter.
    @Test
    public void testCompanyListBeanTypeFullStreaming() throws Exception {
        this.csvCellProcessors = companyListCsvCellProcessors;
        this.csvNameMapping = companyListCsvNameMapping;
        testReadWrite0(streamingWriterTestJobName, companyListCsv, "testCompanyListBeanTypeFullStreaming.xlsx", null,
                templateResource, companyListTemplateSheetName, companyListTemplateHeaderRow,
                Company.class, companyListTemplateSheetName);
    }

    //verifies reading very large csv data set (IBM_unadjusted.txt, 51,058,469) and writing excel (size 34,232,654).
    //when running with ExcelUserModelItemWriter, failed with java.lang.OutOfMemoryError: GC overhead limit exceeded
    //when running with ExcelStreamingItemWriter, passed after 19 seconds
    @Test
    public void testIBMStockTradeBeanTypeFullStreaming() throws Exception {
        this.csvCellProcessors = ibmStockTradeCellProcessors;
        this.csvNameMapping = ibmStockTradeNameMapping;
        this.csvHeaderless = Boolean.TRUE.toString();
        testReadWrite0(streamingWriterTestJobName, ibmStockTradeCsv, "testIBMStockTradeBeanTypeFullStreaming.xlsx", ibmStockTradeHeader,
                null, null, null,
                StockTrade.class, ibmStockTradeSheetName);
    }


    void testReadWrite0(final String jobName, final String resource, final String writeResource, final String header,
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

        if (this.csvCellProcessors != null) {
            params.setProperty(CsvProperties.CELL_PROCESSORS_KEY, this.csvCellProcessors);
        }
        if (this.csvNameMapping != null) {
            params.setProperty(CsvProperties.NAME_MAPPING_KEY, this.csvNameMapping);
        }
        if (this.csvHeaderless != null) {
            params.setProperty(CsvProperties.HEADERLESS_KEY, this.csvHeaderless);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
