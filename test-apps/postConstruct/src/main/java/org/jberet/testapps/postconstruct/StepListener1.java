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

import jakarta.annotation.PostConstruct;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.StepListener;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.testapps.common.PostConstructPreDestroyBase;

@Named
public class StepListener1 extends PostConstructPreDestroyBase implements StepListener {
    @Inject
    @BatchProperty(name = "os.name")
    private String osName;

    @Override
    public void beforeStep() throws Exception {
        addToJobExitStatus("StepListener1.beforeStep");
    }

    @Override
    public void afterStep() throws Exception {
        addToJobExitStatus("StepListener1.afterStep");
        final StepContextImpl stepContextImpl = (StepContextImpl) this.stepContext;
        final AbstractStepExecution stepExecution = stepContextImpl.getStepExecution();
        final BatchStatus batchStatus = this.stepContext.getBatchStatus();
        final Date endTime = stepExecution.getEndTime();
        System.out.printf("%nStepListener1.afterStep, batchStatus=%s, endTime=%s%n%n", batchStatus, endTime);

        if (batchStatus != BatchStatus.COMPLETED) {
            throw new BatchRuntimeException("Expecting BatchStatus.COMPLETED in StepListener1.afterStep(), but got " + batchStatus);
        }
        if (endTime == null) {
            throw new BatchRuntimeException("Expecting a valid end time in StepListener1.afterStep(), but got " + endTime);
        }
    }

    @PostConstruct
    public void ps() {
        System.out.printf("StepListener1 PostConstruct of %s%n", this);
        if (osName == null) {
            throw new BatchRuntimeException("osNmae field has not been initialized when checking from PostConstruct method.");
        }
        addToJobExitStatus("StepListener1.ps");
    }

    @PreDestroy
    public void pd() {
        System.out.printf("StepListener1 PreDestroy of %s%n", this);
        addToJobExitStatus("StepListener1.pd");
    }
}
