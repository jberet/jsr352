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

package org.jberet.testapps.chunkpartition;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class ChunkPartitionIT extends AbstractIT {
    protected int dataCount = 30;
    protected static final String jobXml = "chunkPartition.xml";

    @Test
    public void partitionThreads() throws Exception {
        for (int i = 10; i >= 8; i--) {
            params.setProperty("thread.count", String.valueOf(i));
            params.setProperty("writer.sleep.time", "100");
            startJobAndWait(jobXml);
            Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
            final String exitStatus = stepExecution0.getExitStatus();
            System.out.printf("Step exit status: %s%n", exitStatus);
            Assert.assertEquals(true, exitStatus.startsWith("PASS"));
        }

        params.setProperty("thread.count", "1");
        params.setProperty("skip.thread.check", "true");
        params.setProperty("writer.sleep.time", "0");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
