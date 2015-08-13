/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.throttle;

import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * The test job has a chunk-step with 10 partitions. When the test client starts many jobs one after another,
 * all available threads in the pool may be quickly used up, and by the time some job execution needs to allocate
 * threads to partitions, there is no more threads available, hence the deadlock.
 * <p/>
 * The purpose of the tests is to verify that the batch runtime should be able to detect the potential deadlock risk
 * when starting a new job executions, and queue it up for later execution when threads are available, to avoid deadlock.
 *
 * The batch runtime is configured to have a fixed thread pool of size 10, in src/main/resources/jberet.properties
 *
 * @see <a href="https://issues.jboss.org/browse/JBERET-180">JBERET-180</a>
 */
@Ignore
public class ThrottleIT extends AbstractIT {
    static final String jobName = "throttle";

    @Test
    public void start9() throws Exception {
        runTest(9);
    }

    @Test
    public void start20() throws Exception {
        runTest(20);
    }

    private void runTest(final int count) throws Exception {
        final List<Long> jobExecutionIds = new ArrayList<Long>();
        for (int i = 0; i < count; ++i) {
            jobExecutionIds.add(jobOperator.start(jobName, params));
        }

        for (final Long id : jobExecutionIds) {
            final JobExecutionImpl exe = (JobExecutionImpl) jobOperator.getJobExecution(id);
            awaitTermination(exe);
            Assert.assertEquals(BatchStatus.COMPLETED, exe.getBatchStatus());
        }
        System.out.printf("%nJobExecution ids: %s%n", jobExecutionIds);
    }

}