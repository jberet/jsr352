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

package org.mybatch.tck.impl;

import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.spi.JobExecutionTimeoutException;
import com.ibm.jbatch.tck.spi.JobExecutionWaiter;
import org.mybatch.runtime.JobExecutionImpl;

public final class JobExecutionWaiterImpl implements JobExecutionWaiter {
    private JobExecutionImpl jobExecution;
    private long sleepTime;

    JobExecutionWaiterImpl(long executionId, JobOperator jobOp, long sleepTime) {
        try {
            this.jobExecution = (JobExecutionImpl) jobOp.getJobExecution(executionId);
            this.sleepTime = sleepTime;
        } catch (JobSecurityException e) {
            throw new IllegalStateException("Failed to create JobExecutionWaiterImpl.", e);
        }
    }

    @Override
    public JobExecution awaitTermination() throws JobExecutionTimeoutException {
        System.out.printf("awaitTerminatioin for jobExecution %s, will timeout in %s milliseconds%n",
                jobExecution.getExecutionId(), sleepTime);
        try {
            jobExecution.awaitTerminatioin(sleepTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //unexpected interrup, ignore.
        }
        return jobExecution;
    }
}
