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

package org.jberet.testapps.split;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies split properties referencing, job element transition, and decision following split.
 * <p/>
 * step within a flow within a split can have step-parent, which is a top-level job element;
 */
public class SplitIT extends AbstractIT {
    public SplitIT() {
        params.setProperty("job-param", "job-param");
    }

    @Test
    public void split() throws Exception {
        startJobAndWait("split.xml");
    }

    @Test
    public void splitTerminationStop() throws Exception {
        final String stepExitStatus = "stop";
        params.setProperty("stepExitStatus", stepExitStatus);
        startJobAndWait("splitTerminationStatus");
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assert.assertEquals(stepExitStatus, jobExecution.getExitStatus());

        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assert.assertEquals(stepExitStatus, stepExecution0.getExitStatus());
    }

    @Test
    public void splitTerminationFail() throws Exception {
        final String stepExitStatus = "fail";
        params.setProperty("stepExitStatus", stepExitStatus);
        startJobAndWait("splitTerminationStatus");
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assert.assertEquals(stepExitStatus, stepExecution0.getExitStatus());
    }

    @Test
    public void splitTerminationException() throws Exception {
        params.setProperty("fail", "true");
        startJobAndWait("splitTerminationStatus");
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), stepExecution0.getExitStatus());
        Assert.assertEquals(1, stepExecutions.size());
    }

    @Test
    public void splitTerminationEnd() throws Exception {
        params.setProperty("endCondition", "*");
        startJobAndWait("splitTerminationStatus");
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.toString(), jobExecution.getExitStatus());

        Assert.assertEquals(1, stepExecutions.size());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), stepExecution0.getExitStatus());
    }

    @Test
    public void splitTerminationNext() throws Exception {
        //params.setProperty("endCondition", "not set");
        startJobAndWait("splitTerminationStatus");
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        Assert.assertEquals(2, stepExecutions.size());
        Assert.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.COMPLETED.name(), stepExecution0.getExitStatus());

        Assert.assertEquals(BatchStatus.FAILED, stepExecutions.get(1).getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED.name(), stepExecutions.get(1).getExitStatus());
    }

}
