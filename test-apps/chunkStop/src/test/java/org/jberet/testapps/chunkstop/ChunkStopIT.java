/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.testapps.chunkstop;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import junit.framework.Assert;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChunkStopIT extends AbstractIT {
    public static final long COMPLETION_TIMEOUT = 60;
    protected int dataCount = 30;
    protected static final String jobXml = "chunkStop.xml";
    protected Properties params = new Properties();
    protected JobOperator jobOperator = BatchRuntime.getJobOperator();
    protected long jobExecutionId;
    protected JobExecutionImpl jobExecution;

    @Before
    public void before() {
        params.setProperty("data.count", String.valueOf(dataCount));
    }

    @After
    public void after() {
        params.clear();
    }

    private void startJob() {
        jobExecutionId = jobOperator.start(jobXml, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
    }

    private long getMetric(StepExecution stepExecution, Metric.MetricType type) {
        long result = 0;
        for (Metric m : stepExecution.getMetrics()) {
            if (m.getType() == type) {
                result = m.getValue();
                break;
            }
        }
        return result;
    }

    private void checkMetrics(StepExecution stepExecution, Metric.MetricType type, long expected) {
        Metric[] metrics = stepExecution.getMetrics();
        if (metrics == null || metrics.length == 0) {
            throw new IllegalStateException("Empty " + metrics);
        }
        for (Metric m : metrics) {
            if (m.getType() == type) {
                Assert.assertEquals(expected, m.getValue());
                return;
            }
        }
        throw new IllegalStateException("Didn't find the Metric type " + type + " in metrics: " + String.valueOf(metrics));
    }

    @Test
    public void chunkStopRestart() throws Exception {
        params.setProperty("writer.sleep.time", "500");
        startJob();
        jobOperator.stop(jobExecutionId);
        jobExecution.awaitTerminatioin(COMPLETION_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());

        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        StepExecution stepExecution = stepExecutions.get(0);
        Assert.assertEquals(1, stepExecutions.size());
        //since we called stop right after start, and the writer sleeps before writing data, there should only be 1 write and commit
        checkMetrics(stepExecution, Metric.MetricType.WRITE_COUNT, 1);
        checkMetrics(stepExecution, Metric.MetricType.COMMIT_COUNT, 1);

        long restartedId = jobOperator.restart(jobExecutionId, params);
        JobExecutionImpl jobExecution1 = (JobExecutionImpl) jobOperator.getJobExecution(restartedId);
        jobExecution1.awaitTerminatioin(COMPLETION_TIMEOUT, TimeUnit.SECONDS);

        stepExecution = jobOperator.getStepExecutions(restartedId).get(0);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution1.getBatchStatus());
        Assert.assertTrue(getMetric(stepExecution, Metric.MetricType.READ_COUNT) < dataCount);
    }

    @Test
    public void chunkFailRestart() throws Exception {
        params.setProperty("reader.fail.at", "13");
        startJob();
        jobExecution.awaitTerminatioin(COMPLETION_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        StepExecution stepExecution = jobOperator.getStepExecutions(jobExecutionId).get(0);
        checkMetrics(stepExecution, Metric.MetricType.READ_COUNT, 13);  //reader.fail.at is 0-based, reader.fail.at 13 means 13 successful read
        checkMetrics(stepExecution, Metric.MetricType.WRITE_COUNT, 10);
        checkMetrics(stepExecution, Metric.MetricType.COMMIT_COUNT, 1);

        params.setProperty("reader.fail.at", "3");
        long restartedId = jobOperator.restart(jobExecutionId, params);
        JobExecutionImpl jobExecution1 = (JobExecutionImpl) jobOperator.getJobExecution(restartedId);
        jobExecution1.awaitTerminatioin(COMPLETION_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution1.getBatchStatus());

        stepExecution = jobOperator.getStepExecutions(restartedId).get(0);
        checkMetrics(stepExecution, Metric.MetricType.READ_COUNT, 20);
        checkMetrics(stepExecution, Metric.MetricType.WRITE_COUNT, 20);
        checkMetrics(stepExecution, Metric.MetricType.COMMIT_COUNT, 3);
    }

    @Test
    public void chunkWriterFailRestart() throws Exception {
        params.setProperty("writer.fail.at", "13");
        startJob();
        jobExecution.awaitTerminatioin(COMPLETION_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        StepExecution stepExecution = jobOperator.getStepExecutions(jobExecutionId).get(0);
        checkMetrics(stepExecution, Metric.MetricType.READ_COUNT, 20);
        checkMetrics(stepExecution, Metric.MetricType.WRITE_COUNT, 10);
        checkMetrics(stepExecution, Metric.MetricType.COMMIT_COUNT, 1);

        params.setProperty("writer.fail.at", "-1");
        long restartedId = jobOperator.restart(jobExecutionId, params);
        JobExecutionImpl jobExecution1 = (JobExecutionImpl) jobOperator.getJobExecution(restartedId);
        jobExecution1.awaitTerminatioin(COMPLETION_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution1.getBatchStatus());

        stepExecution = jobOperator.getStepExecutions(restartedId).get(0);
        checkMetrics(stepExecution, Metric.MetricType.READ_COUNT, 20);
        checkMetrics(stepExecution, Metric.MetricType.WRITE_COUNT, 20);
        checkMetrics(stepExecution, Metric.MetricType.COMMIT_COUNT, 3);
    }
}
