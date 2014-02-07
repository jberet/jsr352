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
public final class MovieTest {
    static final String jobName = "org.jberet.support.io.MovieTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    private final String header = "rank,tit,grs,opn";

    static final String cellProcessors =
            "ParseInt; NotNull, StrMinMax(1, 100); DMinMax(1000000, 1000000000); ParseDate(YYYY-MM-dd)";

    static final String expectFull = "Marvel's The Avengers," +
            "Dark Knight Rises," +
            "Chimpanzee," +
            "Five-Year Engagement";

    @Test
    public void testBeanType2_4() throws Exception {
        final String expect = "Dark Knight Rises, " +
                "Hunger Games," +
                "Skyfall";
        final String forbid = "Marvel's The Avengers, " +
                "Hobbit Unexpected Journey";
        testReadWrite0("2", "4", Movie.class, expect, forbid);
    }

    @Test
    public void testListTypeFull() throws Exception {
        testReadWrite0(null, null, List.class, expectFull, null);
    }

    @Test
    public void testBeanTypeFull() throws Exception {
        testReadWrite0(null, null, Movie.class, expectFull, null);
    }

    @Test
    public void testMapTypeFull1_100() throws Exception {
        testReadWrite0("1", "100", Map.class, expectFull, null);
    }

    @Test
    public void testMapType1_2() throws Exception {
        final String expect = "Marvel's The Avengers," +
                "Dark Knight Rises";
        final String forbid = "Hunger Games";
        testReadWrite0("1", "2", Map.class, expect, forbid);
    }

    private void testReadWrite0(final String start, final String end, final Class<?> beanType,
                                final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        params.setProperty("writeResource", CsvProperties.RESOURCE_STEP_CONTEXT);
        params.setProperty(CsvProperties.CELL_PROCESSORS_KEY, cellProcessors);

        if (expect != null) {
            params.setProperty("validate", String.valueOf(true));
            params.setProperty("expect", expect);
        }
        if (forbid != null) {
            params.setProperty("validate", String.valueOf(true));
            params.setProperty("forbid", forbid);
        }

        if (start != null) {
            params.setProperty(CsvProperties.START_KEY, start);
        }
        if (end != null) {
            params.setProperty(CsvProperties.END_KEY, end);
        }

        params.setProperty(CsvProperties.HEADER_KEY, header);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
