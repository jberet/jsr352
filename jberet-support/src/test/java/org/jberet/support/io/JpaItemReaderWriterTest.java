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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;
import javax.persistence.Persistence;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jberet.support.io.JpaResourceProducer.em;
import static org.jberet.support.io.JpaResourceProducer.emf;

public final class JpaItemReaderWriterTest {
    private static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    private static final String jpaItemWriterJob = "org.jberet.support.io.jpaItemWriterTest";
    private static final String jpaItemReaderJob = "org.jberet.support.io.jpaItemReaderTest";
    static final String persistenceUnitName = "JpaItemWriterTest";

    @BeforeClass
    public static void beforeClass() {
        emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        em = emf.createEntityManager();
    }

    @AfterClass
    public static void afterClass() {
        if (em != null) {
            em.close();
        }
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    public void nativeQuery() throws Exception {
        final String testName = "nativeQuery";
        long jobExecutionId = jobOperator.start(jpaItemWriterJob, null);
        JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);

        List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        StepExecutionImpl step1 = (StepExecutionImpl) stepExecutions.get(0);
        System.out.printf("%s, %s, %s%n", step1.getStepName(), step1.getBatchStatus(), step1.getException());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final Properties jobParams = new Properties();
        jobParams.setProperty("resource",
                (new File(CsvItemReaderWriterTest.tmpdir, testName + ".txt")).getPath());
        jobExecutionId = jobOperator.start(jpaItemReaderJob, jobParams);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);

        stepExecutions = jobExecution.getStepExecutions();
        step1 = (StepExecutionImpl) stepExecutions.get(0);
        System.out.printf("%s, %s, %s%n", step1.getStepName(), step1.getBatchStatus(), step1.getException());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

    }

}
