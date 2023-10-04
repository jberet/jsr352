/*
 * Copyright (c) 2013-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunkstop;

import java.util.Properties;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.Metric;

import org.jberet.runtime.JobExecutionImpl;
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
    public void before() throws Exception {
        super.before();
        params.setProperty("data.count", String.valueOf(dataCount));
    }

    @After
    public void after() {
        params.clear();
    }

    @Test
    public void chunkStopRestart() throws Exception {
        params.setProperty("writer.sleep.time", "1000");
        params.setProperty("restartable", Boolean.TRUE.toString());
        startJob(jobXml);
        jobOperator.stop(jobExecutionId);
        awaitTermination();
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());

        Assert.assertEquals(true, stepExecutions.size() < dataCount);

        if(stepExecutions.size() == 1) {
            //since we called stop right after start, and the writer sleeps before writing data, there should only be 1 write and commit
            Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
            Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
        }

        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertTrue(MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT) <= dataCount);
    }

    @Test
    public void restartJobParameters() throws Exception {
        params.setProperty("writer.sleep.time", "500");
        params.setProperty("old.restart.prop.key", "old.restart.prop.val");
        startJob(jobXml);
        jobOperator.stop(jobExecutionId);
        awaitTermination();
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        final Properties parameters1 = jobOperator.getParameters(jobExecutionId);
        System.out.printf("%nstart job parameters: %s%n", parameters1);

        //there may be internal job parameters added, so skip checking its size.
        //Assert.assertEquals(3, parameters1.size());

        params = new Properties();
        params.setProperty("data.count", String.valueOf(dataCount));
        params.setProperty("writer.sleep.time", "501");
        params.setProperty("new.restart.prop.key", "new.restart.prop.val");
        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Properties parameters2 = jobOperator.getParameters(jobExecutionId);
        System.out.printf("%nrestart job parameters: %s%n", parameters2);
        //Assert.assertEquals(4, parameters2.size());

        Assert.assertEquals("old.restart.prop.val", parameters2.getProperty("old.restart.prop.key"));
        Assert.assertEquals("new.restart.prop.val", parameters2.getProperty("new.restart.prop.key"));
        Assert.assertEquals("501", parameters2.getProperty("writer.sleep.time"));
        Assert.assertEquals(String.valueOf(dataCount), parameters2.getProperty("data.count"));
    }

    /**
     * Verifies that restarting with null job parameters should work without causing {@code NullPointerException}.
     * @throws Exception
     */
    @Test
    public void restartWithNullJobParameters() throws Exception {
        params.setProperty("writer.sleep.time", "500");
        startJob(jobXml);
        jobOperator.stop(jobExecutionId);
        awaitTermination();

        final long restartExecutionId = jobOperator.restart(jobExecutionId, null);
        final JobExecutionImpl jobEx = (JobExecutionImpl) jobOperator.getJobExecution(restartExecutionId);
        awaitTermination(jobEx);
        Assert.assertEquals(BatchStatus.COMPLETED, jobEx.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED, jobOperator.getStepExecutions(restartExecutionId).get(0).getBatchStatus());
    }

    @Test
    public void chunkStopAbandon() throws Exception {
        params.setProperty("writer.sleep.time", "500");
        startJob(jobXml);
        jobOperator.stop(jobExecutionId);
        awaitTermination();
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());

        jobOperator.abandon(jobExecutionId);
        Assert.assertEquals(BatchStatus.ABANDONED, jobExecution.getBatchStatus());
    }

    @Test
    public void chunkFailRestart() throws Exception {
        params.setProperty("reader.fail.on.values", "13");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        Assert.assertEquals(13, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));  //reader.fail.at is 0-based, reader.fail.at 13 means 13 successful read
        Assert.assertEquals(10, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));

        params.setProperty("reader.fail.on.values", "3");
        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(3, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));
    }

    @Test
    public void chunkFailUnrestartable() throws Exception {
        params.setProperty("reader.fail.on.values", "13");
        params.setProperty("restartable", Boolean.FALSE.toString());
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        params.setProperty("reader.fail.on.values", "3");
        try {
            restartAndWait();
            Assert.fail("Expecting JobRestartException, but got none.");
        } catch (final JobRestartException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void chunkWriterFailRestart() throws Exception {
        params.setProperty("writer.fail.on.values", "13");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution0, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(10, MetricImpl.getMetric(stepExecution0, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution0, Metric.MetricType.COMMIT_COUNT));

        params.setProperty("writer.fail.on.values", "-1");
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
