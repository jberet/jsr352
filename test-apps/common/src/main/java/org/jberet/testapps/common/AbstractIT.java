/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;

import org.jberet.job.model.Job;
import org.jberet.operations.JobOperatorImpl;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.spi.JobOperatorContext;
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
    protected JobOperatorImpl jobOperator;

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
            // org.jberet.spi.JobOperatorContext and org.jberet.operations.DelegatingJobOperator are introduced
            // in jberet 1.3.0, to allow for different JobOperator implementation in different runtime
            // environment.
            //
            // The BatchRuntime.getJobOperator() returns the DelegatingJobOperator, using the context will give us
            // access to the internal JobOperator implementation class.
            //
            // It's better to cast it to org.jberet.operations.AbstractJobOperator, which is extended
            // by all concrete JobOperator implementation in different runtime environment
            // (Java SE and WildFly Jakarta EE). For example,
            //
            // AbstractJobOperator jobOperator = (AbstractJobOperator) JobOperatorContext.getJobOperatorContext().getJobOperator();
            //
            // Casting to JobOperatorImpl works in Java SE environment, but does not work in WildFly Jakarta EE
            // environment, which has a different JobOperator implementation class.
            //
            jobOperator = (JobOperatorImpl) JobOperatorContext.getJobOperatorContext().getJobOperator();
        }
    }

    protected long getJobTimeoutSeconds() {
        return jobTimeout;
    }

    protected void startJob(final String jobXml) {
        jobExecutionId = jobOperator.start(jobXml, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
    }

    protected void startJob(final Job job) {
        jobExecutionId = jobOperator.start(job, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
    }

    protected void awaitTermination(final JobExecutionImpl... exes) throws InterruptedException {
        final JobExecutionImpl exe = exes.length == 0 ? jobExecution : exes[0];
        exe.awaitTermination(getJobTimeoutSeconds(), TimeUnit.SECONDS);
        stepExecutions = jobOperator.getStepExecutions(exe.getExecutionId());
        if (!stepExecutions.isEmpty()) {
            stepExecution0 = (StepExecutionImpl) stepExecutions.get(0);
        }
    }

    protected void startJobAndWait(final String jobXml) throws Exception {
        startJob(jobXml);
        awaitTermination();
    }

    protected void startJobAndWait(final Job job) throws Exception {
        startJob(job);
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
