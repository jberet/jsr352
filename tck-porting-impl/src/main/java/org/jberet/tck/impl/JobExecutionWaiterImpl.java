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

import com.ibm.jbatch.tck.spi.JobExecutionTimeoutException;
import com.ibm.jbatch.tck.spi.JobExecutionWaiter;
import org.jberet.runtime.JobExecutionImpl;

public final class JobExecutionWaiterImpl implements JobExecutionWaiter {
    private final JobExecutionImpl jobExecution;
    private final long sleepTime;
    private final JobOperator jobOperator;

    JobExecutionWaiterImpl(final long executionId, final JobOperator jobOp, final long sleepTime) {
        try {
            this.jobOperator = jobOp;
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
        try {
            jobExecution.awaitTerminatioin(sleepTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //unexpected interrup, ignore.
        }
        System.out.printf("awaitTerminatioin for jobName %s, jobExecution %s, timeout %s milliseconds, BatchStatus: %s%n",
                jobExecution.getJobName(), jobExecution.getExecutionId(), sleepTime, jobExecution.getBatchStatus());

        return jobExecution;
    }
}
