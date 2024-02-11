/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunk;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ClosingReaderWriterTest {

    static final String XML_NAME = "org.jberet.test.chunkClose";

    private JobOperator jobOperator;
    private Properties params;

    @BeforeEach
    public void setUp() {
        jobOperator = BatchRuntime.getJobOperator();
        params = new Properties();
        params.setProperty("failReaderAtOpen", "false");
        params.setProperty("failWriterAtOpen", "false");
        params.setProperty("stopReadAt", "-1");
        params.setProperty("failReadAt", "-1");
        params.setProperty("failWriteAt", "-1");
    }

    @Test
    public void failReader() throws Exception {
        params.setProperty("failReadAt", "7");
        validate(BatchStatus.FAILED, 7, 6);
    }

    @Test
    public void failReaderAtOpen() throws Exception {
        params.setProperty("failReaderAtOpen", "true");
        validate(BatchStatus.FAILED, 0, 0);
    }

    @Test
    public void failWriter() throws Exception {
        params.setProperty("failWriteAt", "6");
        validate(BatchStatus.FAILED, 6, 6);
    }

    @Test
    public void failWriterAtOpen() throws Exception {
        params.setProperty("failWriterAtOpen", "true");
        validate(BatchStatus.FAILED, 0, 0);
    }

    @Test
    public void complete() throws Exception {
        params.setProperty("stopReadAt", "6");
        validate(BatchStatus.COMPLETED, 6, 5);
    }

    private void validate(final BatchStatus expectedBatchStatus, final int expectedReaderCount, final int expectedWriterCount) throws Exception {
        final long jobId = jobOperator.start(XML_NAME, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertEquals(expectedBatchStatus, jobExecution.getBatchStatus());

        final StepExecution step0 = jobExecution.getStepExecutions().get(0);
        final ReaderWriterResult item = ReaderWriterResult.class.cast(step0.getPersistentUserData());
        Assertions.assertTrue("Reader was not closed", item.isReaderClosed());
        Assertions.assertTrue("Writer was not closed", item.isWriterClosed());
        Assertions.assertEquals("Unexpected reader count.", expectedReaderCount, item.getReadCount());
        Assertions.assertEquals("Unexpected writer count.", expectedWriterCount, item.getWriteCount());
    }
}
