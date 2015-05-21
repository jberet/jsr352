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
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public final class MovieTestWithJacksonCsv extends MovieTest {
    private static final String jobName = "org.jberet.support.io.MovieTestWithJacksonCsv";
    static final File tmpdir = new File(System.getProperty("jberet.tmp.dir"));

    static {
        if (!tmpdir.exists()) {
            tmpdir.mkdirs();
        }
    }

    //@Test
    @Override
    public void testBeanTypeNoDate2_4() throws Exception {
    }

    @Test
    public void testBeanType2_4() throws Exception {
        testReadWrite0("testBeanType2_4.out", "2", "4",
                Movie.class.getName(), true, Movie.class.getName(),
                expect2_4, forbid2_4);
    }

    @Test
    public void testListTypeFull() throws Exception {
        testReadWrite0("testListTypeFull.out", null, null,
                List.class.getName(), false, null,
                expectFull, null);
    }

    @Test
    public void testBeanTypeFull() throws Exception {
        testReadWrite0("testBeanTypeFull.out", null, null,
                Movie.class.getName(), true, Movie.class.getName(),
                expectFull, null);
    }

    @Test
    public void testMapTypeFull1_100() throws Exception {
        testReadWrite0("testMapTypeFull1_100.out", "1", "100",
                //Map.class.getName(), true, null,
                Map.class.getName(), true, header,
                expectFull, null);
    }

    @Test
    public void testMapType1_2() throws Exception {
        testReadWrite0("testMapType1_2.out", "1", "2",
                //Map.class.getName(), true, null,
                Map.class.getName(), true, header,
                expect1_2, forbid1_2);
    }

    private void testReadWrite0(final String writeResource, final String start, final String end,
                                final String beanType, final boolean useHeader, final String columns,
                                final String expect, final String forbid) throws Exception {
        final Properties params = new Properties();
        params.setProperty(CsvProperties.BEAN_TYPE_KEY, beanType);

        if (start != null) {
            params.setProperty("start", start);
        }
        if (end != null) {
            params.setProperty("end", end);
        }
        if (useHeader) {
            params.setProperty("useHeader", String.valueOf(useHeader));
        }
        if (columns != null) {
            params.setProperty("columns", columns);
        }

        final File writeResourceFile = new File(tmpdir, writeResource);
        params.setProperty("writeResource", writeResourceFile.getPath());

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        CsvItemReaderWriterTest.validate(writeResourceFile, expect, forbid);
    }
}
