/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.common;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.junit.Assert;
import org.junit.Before;

abstract public class AbstractIT {
    /**
     * Common job xml for infinispan job repository tests.
     */
    protected static final String infinispanRepositoryJobXml = "org.jberet.test.infinispanRepository";

    /**
     * Saves the default locale before temporarily switching to {@code Locale.US}. Some tests (e.g., PropertyInjectionIT,
     * PostConstructIT) uses US date format, so need to switch Locale.US to run these tests.
     */
    private static Locale originalLocale;

    protected long jobTimeout;

    protected Properties params = new Properties();
    protected long jobExecutionId;
    protected JobExecutionImpl jobExecution;
    protected List<StepExecution> stepExecutions;
    protected StepExecutionImpl stepExecution0;

    //delay bootstrapping JobOperator, since some tests may need to adjust jberet configuration, such as
    //infinispanRepository tests.
    protected JobOperator jobOperator;

    /**
     * Initializes and bootstraps {@code JobOperator}.
     * It appears JUnit will not invoke {@code @Before} methods on the superclass if the subclass also defines its own
     * {@code @Before} method.
     * Therefore any {@code @Before} method on subclass must first invoke {@code super.before()}.
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        if (jobOperator == null) {
            jobOperator = BatchRuntime.getJobOperator();
        }
    }

    protected long getJobTimeoutSeconds() {
        return jobTimeout;
    }

    protected void startJob(final String jobXml) {
        jobExecutionId = jobOperator.start(jobXml, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
    }

    protected void awaitTermination(final JobExecutionImpl... exes) throws InterruptedException {
        final JobExecutionImpl exe = exes.length == 0 ? jobExecution : exes[0];
        exe.awaitTermination(getJobTimeoutSeconds(), TimeUnit.SECONDS);
        stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        stepExecution0 = (StepExecutionImpl) stepExecutions.get(0);
    }

    protected void startJobAndWait(final String jobXml) throws Exception {
        startJob(jobXml);
        awaitTermination();
    }

    protected void restartAndWait(final long... oldJobExecutionIds) throws InterruptedException {
        final long restartId = oldJobExecutionIds.length == 0 ? jobExecutionId : oldJobExecutionIds[0];
        jobExecutionId = jobOperator.restart(restartId, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        awaitTermination();
    }

    protected long getOriginalJobExecutionId(final String jobName) {
        final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, 0, 1);
        final JobInstance jobInstance = jobInstances.get(0);
        final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstance);
        final JobExecution originalJobExecution = jobExecutions.get(jobExecutions.size() - 1);
        return originalJobExecution.getExecutionId();
    }

    protected static void switchToUSLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    protected static void restoreDefaultLocale() {
        Locale.setDefault(originalLocale);
    }

    protected void verifyMetric(final Metric.MetricType metricType, final long value) {
        boolean metricFound = false;
        final Metric[] metrics = stepExecution0.getMetrics();
        for (final Metric m : metrics) {
            if (m.getType() == metricType) {
                metricFound = true;
                Assert.assertEquals(value, m.getValue());
            }
        }
        if (!metricFound) {
            throw new IllegalStateException("Unmatched MetricType " + metricType);
        }
    }

    protected void assertNoSuchJobExecution(final long i) {
        try {
            final JobExecution j = jobOperator.getJobExecution(i);
            Assert.fail("Expecting NoSuchJobExecutionException, but got " + j);
        } catch (final NoSuchJobExecutionException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }
}
