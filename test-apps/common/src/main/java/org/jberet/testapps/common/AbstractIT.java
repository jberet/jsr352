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

package org.jberet.testapps.common;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;

abstract public class AbstractIT {
    protected long jobTimeout = Long.getLong(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_KEY, JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT);

    protected Properties params = new Properties();
    protected JobOperator jobOperator = BatchRuntime.getJobOperator();
    protected long jobExecutionId;
    protected JobExecutionImpl jobExecution;
    protected List<StepExecution> stepExecutions;
    protected StepExecution stepExecution0;

    protected void startJob(String jobXml) {
        jobExecutionId = jobOperator.start(jobXml, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
    }

    protected void awaitTermination(JobExecutionImpl... exes) throws InterruptedException {
        JobExecutionImpl exe = exes.length == 0 ? jobExecution : exes[0];
        exe.awaitTerminatioin(jobTimeout, TimeUnit.SECONDS);
        stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        stepExecution0 = stepExecutions.get(0);
    }

    protected void startJobAndWait(String jobXml) throws Exception {
        startJob(jobXml);
        awaitTermination();
    }

    protected void restartAndWait(long... oldJobExecutionIds) throws InterruptedException {
        long restartId = oldJobExecutionIds.length == 0 ? jobExecutionId : oldJobExecutionIds[0];
        jobExecutionId = jobOperator.restart(restartId, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        awaitTermination();
    }
}
