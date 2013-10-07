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

package org.jberet.tck.impl;

import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.spi.JobExecutionTimeoutException;
import com.ibm.jbatch.tck.spi.JobExecutionWaiter;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;

public final class JobExecutionWaiterImpl implements JobExecutionWaiter {
    private final JobExecutionImpl jobExecution;
    private final long sleepTime;

    JobExecutionWaiterImpl(final long executionId, final JobOperator jobOp, final long sleepTime) {
        try {
            this.jobExecution = (JobExecutionImpl) jobOp.getJobExecution(executionId);
            this.sleepTime = sleepTime;
        } catch (JobSecurityException e) {
            throw new IllegalStateException("Failed to create JobExecutionWaiterImpl.", e);
        } catch (NoSuchJobExecutionException e) {
            throw new IllegalStateException("Failed to create JobExecutionWaiterImpl.", e);
        }
    }

    @Override
    public JobExecution awaitTermination() throws JobExecutionTimeoutException {
        System.out.printf("Before awaitTermination for JobExecution %s, timeout %d%n", jobExecution, sleepTime);
        try {
            jobExecution.awaitTermination(sleepTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //unexpected interrupt, ignore.
            e.printStackTrace();
        }
        System.out.printf("After awaitTermination for jobName %s, jobExecution %s, BatchStatus %s, StepExecutions %s%n",
                jobExecution.getJobName(), jobExecution.getExecutionId(), jobExecution.getBatchStatus(),
                jobExecution.getStepExecutions());

        for (final StepExecution e : jobExecution.getStepExecutions()) {
            final StepExecutionImpl e2 = (StepExecutionImpl) e;
            System.out.printf("StepExecution %s, batch status %s, exit status %s, exception %s%n",
                    e2, e2.getBatchStatus(), e2.getExitStatus(), e2.getException());
        }

        return jobExecution;
    }
}
