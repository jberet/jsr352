/*
 * Copyright (c) 2022 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import java.util.logging.Logger;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Tests to verify various injection mechanisms (field-, constructor-, and method-injection).
 * Modelled after batch tck class {@code com.ibm.jbatch.tck.artifacts.cdi.DependentScopedBatchletContexts}
 */
@Named
@Dependent
public class InjectBatchlet extends AbstractBatchlet implements Batchlet {
    Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    JobContext jf;
    JobContext jc;
    JobContext jm;
    @Inject
    StepContext sf;
    StepContext sc;
    StepContext sm;

    @Inject
    InjectBatchlet(JobContext jc, StepContext sc) {
        logger.info("this.jc: " + this.jc + ", jc: " + jc);
        logger.info("this.sc: " + this.sc + ", sc: " + sc);
        this.jc = jc;
        this.sc = sc;
    }

    @Inject
    public void setMethod1(JobContext jm) {
        logger.info("this.jm: " + this.jm + ", jm: " + jm);
        this.jm = jm;
    }

    @Inject
    public void setMethod2(StepContext sm) {
        logger.info("this.sm: " + this.sm + ", sm: " + sm);
        this.sm = sm;
    }

    @Override
    public String process() throws Exception {
        updateJobExitStatus(jf);
        updateJobExitStatus(jc);
        updateJobExitStatus(jm);

        updateStepExitStatus(sf);
        updateStepExitStatus(sc);
        return updateStepExitStatus(sm);
    }

    private void updateJobExitStatus(JobContext jobCtx) {
        String es = jobCtx.getExitStatus();
        jobCtx.setExitStatus((es == null ? "" : es) + jobCtx.getExecutionId() + ":");
    }

    private String updateStepExitStatus(StepContext stepCtx) {
        String es = stepCtx.getExitStatus();
        es = (es == null ? "" : es) + stepCtx.getStepExecutionId() + ":";
        stepCtx.setExitStatus(es);
        return es;
    }

}
