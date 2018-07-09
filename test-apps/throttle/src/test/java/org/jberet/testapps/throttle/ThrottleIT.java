/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.throttle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.batch.runtime.BatchStatus;

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
 * <p/>
 * The batch runtime is configured to have a fixed thread pool of size 10, in src/main/resources/jberet.properties
 *
 * @see <a href="https://issues.jboss.org/browse/JBERET-180">JBERET-180</a>
 */
public class ThrottleIT extends AbstractIT {
    static final String jobName = "throttle";

    /**
     * Starts the test job 300 times, one after another or concurrently.
     * The test has a chunk-type step with 10 partitioins.
     * Verifies the job submission is properly throttled and the these jobs should complete successfully without deadlock.
     *
     * @throws Exception
     */
    @Test
    public void start300() throws Exception {
        runTest(300, true);
    }

    @Ignore
    @Test
    public void start1000() throws Exception {
        runTest(1000, true);
    }

    @Ignore
    @Test
    public void start2100() throws Exception {
        runTest(2100, true);
    }

    /**
     *
     * @param count number of times to start the test job
     * @param concurrent a flag whether to start the test job serially or concurently
     *
     * @throws Exception
     */
    private void runTest(final int count, final boolean concurrent) throws Exception {
        final List<Long> jobExecutionIds = new CopyOnWriteArrayList<Long>();

        if(concurrent) {
            final List<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < count; i++) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        jobExecutionIds.add(jobOperator.start(jobName, params));
                    }
                });
                threads.add(t);
                t.start();
            }
            for (final Thread t : threads) {
                t.join();
            }
        } else {
            for (int i = 0; i < count; ++i) {
                jobExecutionIds.add(jobOperator.start(jobName, params));
            }
        }

        for (final Long id : jobExecutionIds) {
            final JobExecutionImpl exe = (JobExecutionImpl) jobOperator.getJobExecution(id);
            awaitTermination(exe);
            Assert.assertEquals(BatchStatus.COMPLETED, exe.getBatchStatus());
        }
        System.out.printf("%nJobExecution ids: %s%n", jobExecutionIds);
    }

}