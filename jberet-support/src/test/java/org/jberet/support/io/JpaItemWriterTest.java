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
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public final class JpaItemWriterTest extends MovieTest {
    private static final String jobName = "org.jberet.support.io.jpaItemWriterTest";
    static final File tmpdir = new File(System.getProperty("jberet.tmp.dir"));

    static {
        if (!tmpdir.exists()) {
            tmpdir.mkdirs();
        }
    }

    @Test
    public void testBeanTypeFull() throws Exception {
        final long jobExecutionId = jobOperator.start(jobName, null);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(5, TimeUnit.MINUTES);

        final List<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        final StepExecutionImpl step1 = (StepExecutionImpl) stepExecutions.get(0);
        System.out.printf("%s, %s, %s%n", step1.getStepName(), step1.getBatchStatus(), step1.getException());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

    }

}
