/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
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

public class KafkaReaderWriterTest {
    private static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.KafkaWriterTest.xml";
    static final String readerTestJobName = "org.jberet.support.io.KafkaReaderTest.xml";

    static final String ibmStockTradeExpected1_10 = "09:30, 67040, 09:31, 10810,    09:39, 2500";
    static final String ibmStockTradeForbid1_10 = "09:40";

    static final String ibmStockTradeCellProcessorsDateAsString =
            "null; null; ParseDouble; ParseDouble; ParseDouble; ParseDouble; ParseDouble";

    static final String producerRecordKey = null;
    static final String pollTimeout = String.valueOf(10000);

    @Test
    public void readIBMStockTradeCsvWriteKafkaBeanType() throws Exception {
        String topicPartition = "readIBMStockTradeCsvWriteKafkaBeanType" + System.currentTimeMillis() + ":0";
        testWrite0(writerTestJobName, StockTrade.class,
                ExcelWriterTest.ibmStockTradeHeader, ExcelWriterTest.ibmStockTradeCellProcessors,
                "1", "10", topicPartition, producerRecordKey);

        // CsvItemReaderWriter uses header "Date, Time, Open, ..."
        // CsvItemReaderWriter has nameMapping "date, time, open, ..." to match java fields in StockTrade. CsvItemReaderWriter
        // does not understand Jackson mapping annotations in POJO.

        testRead0(readerTestJobName, StockTrade.class, "readIBMStockTradeCsvWriteJmsBeanType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                topicPartition, pollTimeout,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }


    static void testWrite0(final String jobName, final Class<?> beanType, final String csvNameMapping, final String cellProcessors,
                    final String start, final String end,
                    final String topicPartition, final String recordKey) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (cellProcessors != null) {
            params.setProperty("cellProcessors", cellProcessors);
        }
        if (start != null) {
            params.setProperty("start", start);
        }
        if (end != null) {
            params.setProperty("end", end);
        }
        if (topicPartition != null) {
            params.setProperty("topicPartition", topicPartition);
        }
        if (recordKey != null) {
            params.setProperty("recordKey", recordKey);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    static void testRead0(final String jobName, final Class<?> beanType, final String writeResource,
                   final String csvNameMapping, final String csvHeader,
                   final String topicPartitions, final String pollTimeout,
                   final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        final File writeResourceFile;
        if (writeResource != null) {
            writeResourceFile = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
            params.setProperty("writeResource", writeResourceFile.getPath());
        } else {
            throw new RuntimeException("writeResource is null");
        }
        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (csvHeader != null) {
            params.setProperty("header", csvHeader);
        }
        if (topicPartitions != null) {
            params.setProperty("topicPartitions", topicPartitions);
        }
        if (pollTimeout != null) {
            params.setProperty("pollTimeout", pollTimeout);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        CsvItemReaderWriterTest.validate(writeResourceFile, expect, forbid);
    }
}
