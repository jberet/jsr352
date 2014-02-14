/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.chunkstop;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.jberet.runtime.metric.MetricImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ChunkStopIT extends AbstractIT {
    protected int dataCount = 30;
    protected static final String jobXml = "chunkStop.xml";

    @Before
    public void before() {
        params.setProperty("data.count", String.valueOf(dataCount));
    }

    @After
    public void after() {
        params.clear();
    }

    @Test
    public void chunkStopRestart() throws Exception {
        params.setProperty("writer.sleep.time", "500");
        startJob(jobXml);
        jobOperator.stop(jobExecutionId);
        awaitTermination();
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());

        Assert.assertEquals(1, stepExecutions.size());
        //since we called stop right after start, and the writer sleeps before writing data, there should only be 1 write and commit
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));

        restartAndWait();

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertTrue(MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT) < dataCount);
    }

    @Test
    public void chunkFailRestart() throws Exception {
        params.setProperty("reader.fail.at", "13");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        Assert.assertEquals(13, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));  //reader.fail.at is 0-based, reader.fail.at 13 means 13 successful read
        Assert.assertEquals(10, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));

        params.setProperty("reader.fail.at", "3");
        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(3, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
    }

    @Test
    public void chunkWriterFailRestart() throws Exception {
        params.setProperty("writer.fail.at", "13");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(10, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));

        params.setProperty("writer.fail.at", "-1");
        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(3, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
    }

    @Test
    public void skippableExceptions() throws Exception {
        params.setProperty("data.count", "1");
        params.setProperty("throwException", "true");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(1, stepExecutions.size());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), stepExecution0.getExitStatus());
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.PROCESS_SKIP_COUNT));
    }
}
