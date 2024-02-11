/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.split;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies split properties referencing, job element transition, and decision following split.
 * <p>
 * step within a flow within a split can have step-parent, which is a top-level job element;
 * <p>
 * split with no timeout, and split with timeout configured with either job parameters or job properties.
 *
 * @see org.jberet.spi.PropertyKey#SPLIT_TIMEOUT_SECONDS
 */
public class SplitIT extends AbstractIT {
    private static final String splitXml = "split.xml";
    private static final String splitTerminationStatusXml = "splitTerminationStatus.xml";
    private static final String splitTimeoutPropertyXml = "splitTimeoutProperty.xml";
    private static final String splitWithoutTimeoutPropertyXml = "splitWithoutTimeoutProperty.xml";

    public SplitIT() {
        params.setProperty("job-param", "job-param");
    }

    @Test
    public void splitTimeoutProperty() throws Exception {
        //use the split timeout property configured in job.xml, which is shorter than
        //batchlet2 sleep time, and so the split and job execution will timeout and fail
        params = null;
        startJobAndWait(splitTimeoutPropertyXml);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void splitTimeoutPropertyOverrideByJobParameter() throws Exception {
        //the shorter split timeout property in job.xml is overridden by the same-named job parameter to be longer,
        //so the split and job execution will wait sufficiently long to complete
        params.setProperty("jberet.split.timeout.seconds", String.valueOf(7));
        startJobAndWait(splitTimeoutPropertyXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());
    }

    @Test
    public void splitWithoutTimeoutProperty() throws Exception {
        //no split timeout value is configured anywhere, the split execution should wait for flow to complete
        params = null;
        startJobAndWait(splitWithoutTimeoutPropertyXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());
    }

    @Test
    public void splitWithoutTimeoutPropertyOverrideByJobParameter() throws Exception {
        //no split timeout is configured in job.xml, but it is configured in job parameter to be sufficiently long,
        //so the split and job execution will wait long enough and complete
        params.setProperty("jberet.split.timeout.seconds", String.valueOf(7));
        startJobAndWait(splitWithoutTimeoutPropertyXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());
    }

    @Test
    public void splitWithoutTimeoutPropertyOverrideByJobParameterShorter() throws Exception {
        //no split timeout is configured in job.xml, but is configured in job parameter to be shorter,
        //so the split and job execution will just timeout and fail
        params.setProperty("jberet.split.timeout.seconds", String.valueOf(3));
        startJobAndWait(splitWithoutTimeoutPropertyXml);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void split() throws Exception {
        startJobAndWait(splitXml);
    }

    @Test
    public void splitTerminationStop() throws Exception {
        final String stepExitStatus = "stop";
        params.setProperty("stepExitStatus", stepExitStatus);
        startJobAndWait(splitTerminationStatusXml);
        Assertions.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assertions.assertEquals(stepExitStatus, jobExecution.getExitStatus());

        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assertions.assertEquals(stepExitStatus, stepExecution0.getExitStatus());
    }

    @Test
    public void splitTerminationFail() throws Exception {
        final String stepExitStatus = "fail";
        params.setProperty("stepExitStatus", stepExitStatus);
        startJobAndWait(splitTerminationStatusXml);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assertions.assertEquals(stepExitStatus, stepExecution0.getExitStatus());
    }

    @Test
    public void splitTerminationException() throws Exception {
        params.setProperty("fail", "true");
        startJobAndWait(splitTerminationStatusXml);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        Assertions.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), stepExecution0.getExitStatus());
        Assertions.assertEquals(1, stepExecutions.size());
    }

    @Test
    public void splitTerminationEnd() throws Exception {
        params.setProperty("endCondition", "*");
        startJobAndWait(splitTerminationStatusXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), jobExecution.getExitStatus());

        Assertions.assertEquals(1, stepExecutions.size());
        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), stepExecution0.getExitStatus());
    }

    @Test
    public void splitTerminationNext() throws Exception {
        //params.setProperty("endCondition", "not set");
        startJobAndWait(splitTerminationStatusXml);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), jobExecution.getExitStatus());

        Assertions.assertEquals(2, stepExecutions.size());
        Assertions.assertEquals(BatchStatus.COMPLETED, stepExecution0.getBatchStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED.name(), stepExecution0.getExitStatus());

        Assertions.assertEquals(BatchStatus.FAILED, stepExecutions.get(1).getBatchStatus());
        Assertions.assertEquals(BatchStatus.FAILED.name(), stepExecutions.get(1).getExitStatus());
    }

}
