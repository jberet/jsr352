/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.inheritance;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

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
