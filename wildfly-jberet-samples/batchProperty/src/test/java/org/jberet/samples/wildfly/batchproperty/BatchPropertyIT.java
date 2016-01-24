/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.batchproperty;

import javax.batch.runtime.BatchStatus;

import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Assert;
import org.junit.Test;

public class BatchPropertyIT extends BatchTestBase {
    private static final String jobName = "batchproperty";

    @Test
    public void testBatchProperty() throws Exception {
        final JobExecutionEntity jobExecution = startJob(jobName, null);
        Thread.sleep(500);
        final JobExecutionEntity jobExecution2 = getJobExecution(jobExecution.getExecutionId());
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution2.getBatchStatus());
    }

    @Override
    protected String getRestUrl() {
        return BASE_URL + "batchproperty/api";
    }
}
