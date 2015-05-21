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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * A test class that reads CSV resource from http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=csv
 */
public class MovieTest {
    private static final String jobName = "org.jberet.support.io.MovieTest";
    static final String moviesCsv = "movies-2012.csv";
    final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String header = "rank,tit,grs,opn";

    static final String cellProcessors =
            "ParseInt; NotNull, StrMinMax(1, 100); DMinMax(1000000, 1000000000); ParseDate(YYYY-MM-dd)";

    //in xml output, ' in Marvel's will be escaped so we cannot match it verbatim
    static final String expectFull = "The Avengers," +
            "The Dark Knight Rises," +
            "Chimpanzee," +
            "The Five-Year Engagement";
    static final String expect2_4 = "The Dark Knight Rises, " +
            "The Hunger Games," +
            "Skyfall";
    static final String forbid2_4 = "The Avengers, " +
            "The Hobbit: An Unexpected Journey";

    static final String expect1_2 = "The Avengers," +
            "The Dark Knight Rises";
    static final String forbid1_2 = "Hunger Games";

    private String partialNameMapping;

    //test partial reading (certain columns are not read by include null in nameMapping for these columns).
    //for bean type reading only.
    @Test
    public void testBeanTypeNoDate2_4() throws Exception {
        this.partialNameMapping = "rank,tit,grs,null";
        testReadWrite0("testBeanTypeNoDate2_4.out", "2", "4", Movie.class, expect2_4, forbid2_4 + ", 2012");
        this.partialNameMapping = null;
    }

    @Test
    public void testBeanType2_4() throws Exception {
        testReadWrite0("testBeanType2_4.out", "2", "4", Movie.class, expect2_4, forbid2_4);
    }

    @Test
    public void testListTypeFull() throws Exception {
        testReadWrite0("testListTypeFull.out", null, null, List.class, expectFull, null);
    }

    @Test
    public void testBeanTypeFull() throws Exception {
        testReadWrite0("testBeanTypeFull.out", null, null, Movie.class, expectFull, null);
    }

    @Test
    public void testMapTypeFull1_100() throws Exception {
        testReadWrite0("testMapTypeFull1_100.out", "1", "100", Map.class, expectFull, null);
    }

    @Test
    public void testMapType1_2() throws Exception {
        testReadWrite0("testMapType1_2.out", "1", "2", Map.class, expect1_2, forbid1_2);
    }

    private void testReadWrite0(final String writeResource, final String start, final String end, final Class<?> beanType,
                                final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        final File writeResourceFile = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
        params.setProperty("writeResource", writeResourceFile.getPath());
        params.setProperty(CsvProperties.CELL_PROCESSORS_KEY, cellProcessors);

        if (start != null) {
            params.setProperty(CsvProperties.START_KEY, start);
        }
        if (end != null) {
            params.setProperty(CsvProperties.END_KEY, end);
        }
        if (this.partialNameMapping != null) {
            params.setProperty(CsvProperties.NAME_MAPPING_KEY, partialNameMapping);
        }
        params.setProperty(CsvProperties.HEADER_KEY, header);
        CsvItemReaderWriterTest.setRandomWriteMode(params);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        CsvItemReaderWriterTest.validate(writeResourceFile, expect, forbid);
    }
}
