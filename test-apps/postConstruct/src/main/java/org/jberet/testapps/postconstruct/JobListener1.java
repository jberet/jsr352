/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.postconstruct;

import java.util.Date;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.annotation.PostConstruct;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import org.jberet.testapps.common.PostConstructPreDestroyBase;

@Named
public class JobListener1 extends PostConstructPreDestroyBase implements JobListener {
    @Inject
    @BatchProperty(name = "os.name")
    private String osName;

    @Override
    public void beforeJob() throws Exception {
        addToJobExitStatus("JobListener1.beforeJob");
    }

    @Override
    public void afterJob() throws Exception {
        addToJobExitStatus("JobListener1.afterJob");
        final long executionId = jobContext.getExecutionId();
        final JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution(executionId);
        final BatchStatus batchStatus = jobContext.getBatchStatus();
        final Date endTime = jobExecution.getEndTime();
        System.out.printf("%nJobListener1.afterJob, batchStatus=%s, endTime=%s%n%n", batchStatus, endTime);

        if (batchStatus != BatchStatus.COMPLETED) {
            throw new BatchRuntimeException("Expecting BatchStatus.COMPLETED in JobListener1.afterJob(), but got " + batchStatus);
        }
        if (endTime == null) {
            throw new BatchRuntimeException("Expecting a valid end time in JobListener1.afterJob(), but got " + endTime);
        }
    }

    @PostConstruct
    public void ps() {
        System.out.printf("JobListener1 PostConstruct of %s%n", this);
        if (osName == null) {
            throw new BatchRuntimeException("osNmae field has not been initialized when checking from PostConstruct method.");
        }
        addToJobExitStatus("JobListener1.ps");
    }

    @PreDestroy
    public void pd() {
        System.out.printf("JobListener1 PreDestroy of %s%n", this);
        addToJobExitStatus("JobListener1.pd");
    }
}
