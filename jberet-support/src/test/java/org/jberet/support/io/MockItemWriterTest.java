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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A test class that writes batch data to {@link MockItemWriter}.
 */
public final class MockItemWriterTest {
    static final String jobName = "org.jberet.support.io.MockItemWriterTest";
    private static final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Test
    public void toConsoleDefault() throws Exception {
        verifyJobExecution(jobOperator.start(jobName, null), BatchStatus.COMPLETED);
    }

    @Test
    public void toConsole() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("toConsole", Boolean.TRUE.toString());
        verifyJobExecution(jobOperator.start(jobName, jobParams), BatchStatus.COMPLETED);
    }

    @Test
    public void noop() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("toConsole", Boolean.FALSE.toString());
        verifyJobExecution(jobOperator.start(jobName, jobParams), BatchStatus.COMPLETED);
    }

    @Test
    public void toFile() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("toFile",
            new File(CsvItemReaderWriterTest.tmpdir, "MockItemWriterTest-toFile.txt").getAbsolutePath());
        verifyJobExecution(jobOperator.start(jobName, jobParams), BatchStatus.COMPLETED);
    }

    @Test
    public void toClass() throws Exception {
        if (DataHolder.data != null) {
            DataHolder.data.clear();
        }
        final Properties jobParams = new Properties();
        jobParams.setProperty("toClass", DataHolder.class.getName());

        verifyJobExecution(jobOperator.start(jobName, jobParams), BatchStatus.COMPLETED);
        Assert.assertEquals(true, DataHolder.data.size() > 0);

        System.out.printf("data list size : %s, first item: %s%n",
                DataHolder.data.size(), DataHolder.data.get(0));

    }

    @Test
    public void toClassUninitialized() throws Exception {
        if (DataHolderUninitialized.data != null) {
            DataHolderUninitialized.data.clear();
        }
        final Properties jobParams = new Properties();
        jobParams.setProperty("toClass", DataHolderUninitialized.class.getName());

        verifyJobExecution(jobOperator.start(jobName, jobParams), BatchStatus.COMPLETED);
        Assert.assertEquals(true, DataHolderUninitialized.data.size() > 0);

        System.out.printf("data list size : %s, first item: %s%n",
                DataHolderUninitialized.data.size(), DataHolderUninitialized.data.get(0));

    }

    public static void verifyJobExecution(final long jobExecutionId, final BatchStatus expected)
            throws Exception {
        final JobExecutionImpl jobExecution =
                (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(expected, jobExecution.getBatchStatus());
    }


    public static final class DataHolder {
        public static final List data = new ArrayList();
    }

    public static final class DataHolderUninitialized {
        public static volatile List data;
    }
}
