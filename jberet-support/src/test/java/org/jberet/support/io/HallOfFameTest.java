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
 * A test class that reads HallOfFame.txt with bean, list, and map type readers and writes out the original content.
 * HallOfFame.txt is a headerless CSV file.
 */
public final class HallOfFameTest {
    static final String jobName = "org.jberet.support.io.HallOfFame";
    static final String hallOfFameResource = "HallOfFame.txt";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writeComments = "# Comments written by csv writer.";

    static final String nameMapping = "hofID, yearID, votedBy, ballots, needed, votes, inducted, category";

    //constraints as per HallOfFame.sql
    static final String cellProcessors =
            "NotNull, StrMinMax(1, 10); " +    //hofID, enforce range
                    "NotNull, ParseInt;" +   //yearId, parse to int
                    "NotNull, StrMinMax(1, 64); " +  //votedBy
                    "Optional, ParseInt; " +  //ballots, parse to int
                    "Optional; " +     //needed
                    "Optional, DMinMax(0, 1000);" +      //votes, parse to double and enforce range
                    "Optional, ParseBool; " +      //inducted, boolean
                    "Optional, ParseEnum(org.jberet.support.io.HallOfFame$Category); ";      //category, enum

    static final String expectFull = "aaronha01h,1982,BBWAA,415,312,406.0," +
            "harrile01h,2011,BBWAA,581,436";

    @Test
    public void testBeanType() throws Exception {
        final String expect = "abbotji01h, 2005, BBWAA, 516, 387, 13.0, " +
                "adamsba01h, 1937, 201, 151, 8.0, " +
                "adamsba01h, 1938, 262, 197, 11.0";
        final String forbid = "aaronha01h, 1982, 415, 312, 406.0, " +
                "1939, 274, 206";
        testReadWrite0(hallOfFameResource, "2", "4", CsvProperties.RESOURCE_STEP_CONTEXT, HallOfFame.class, expect, forbid);
    }

    @Test
    public void testListTypeFull() throws Exception {
        testReadWrite0(hallOfFameResource, null, null, CsvProperties.RESOURCE_STEP_CONTEXT, List.class, expectFull, null);
    }

    @Test
    public void testBeanTypeFull() throws Exception {
        testReadWrite0(hallOfFameResource, null, null, CsvProperties.RESOURCE_STEP_CONTEXT, HallOfFame.class, expectFull, null);
    }

    @Test
    public void testMapTypeFull() throws Exception {
        testReadWrite0(hallOfFameResource, "1", "9999999", CsvProperties.RESOURCE_STEP_CONTEXT, Map.class, expectFull, null);
    }

    @Test
    public void testMapType() throws Exception {
        final String expect = "aaronha01h,1982,BBWAA,415,312,406.0," +
                "abbotji01h, 2005, BBWAA, 516, 387, 13.0, " +
                "adamsba01h, 1937, 201, 151, 8.0, " +
                "adamsba01h, 1938, 262, 197, 11.0";
        final String forbid = "1939, 274, 206";
        testReadWrite0(hallOfFameResource, "1", "4", CsvProperties.RESOURCE_STEP_CONTEXT, java.util.Map.class, expect, forbid);
    }

    //test will print out the path of output file from CsvItemWriter, which can then be verified.
    //e.g., CSV resource to read:
    //person.csv,
    //to write:
    //        /var/folders/s3/2m3bc7_n0550tp44h4bcgwtm0000gn/T/testMapType.out
    private void testReadWrite0(final String resource, final String start, final String end,
                                final String writeResource, final Class<?> beanType,
                                final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);
        params.setProperty(CsvProperties.CELL_PROCESSORS_KEY, cellProcessors);

        final String writeResourceFullPath = writeResource.equalsIgnoreCase(CsvProperties.RESOURCE_STEP_CONTEXT) ?
                writeResource : new File(CsvItemReaderWriterTest.tmpdir, writeResource).getPath();
        params.setProperty("writeResource", writeResourceFullPath);

        if (beanType != List.class) {
            //nameMapping is not required for list type reader
            params.setProperty(CsvProperties.NAME_MAPPING_KEY, nameMapping);
        }

        if (writeResource.equalsIgnoreCase(CsvProperties.RESOURCE_STEP_CONTEXT)) {
            if (expect != null) {
                params.setProperty("validate", String.valueOf(true));
                params.setProperty("expect", expect);
            }
            if (forbid != null) {
                params.setProperty("validate", String.valueOf(true));
                params.setProperty("forbid", forbid);
            }
        }

        if (start != null) {
            params.setProperty(CsvProperties.START_KEY, start);
        }
        if (end != null) {
            params.setProperty(CsvProperties.END_KEY, end);
        }

        params.setProperty(CsvProperties.HEADER_KEY, nameMapping);
        params.setProperty(CsvProperties.WRITE_COMMENTS_KEY, writeComments);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
