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

package org.jberet.testapps.common;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;

abstract public class AbstractIT {
    protected long jobTimeout;

    protected Properties params = new Properties();
    protected JobOperator jobOperator = BatchRuntime.getJobOperator();
    protected long jobExecutionId;
    protected JobExecutionImpl jobExecution;
    protected List<StepExecution> stepExecutions;
    protected StepExecution stepExecution0;

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
        stepExecution0 = stepExecutions.get(0);
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
}
