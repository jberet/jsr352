/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.inheritance;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class InheritanceIT extends AbstractIT {
    public InheritanceIT() {
        //params.setProperty("job-param", "job-param");
    }

    //abstract step and flow should be skipped when determing the first execution element of a job
    @Test
    public void inheritance() throws Exception {
        startJobAndWait("inheritance.xml");
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(2, jobExecution.getStepExecutions().size());

        Assert.assertEquals("step1", stepExecution0.getStepName());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());

        final StepExecution stepExecution1 = jobExecution.getStepExecutions().get(1);
        Assert.assertEquals("flow0.step1", stepExecution1.getStepName());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution1.getBatchStatus());
    }
}
