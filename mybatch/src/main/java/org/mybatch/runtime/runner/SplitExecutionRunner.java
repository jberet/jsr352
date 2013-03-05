/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.runtime.runner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;

import org.mybatch.job.Flow;
import org.mybatch.job.Split;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.SplitContextImpl;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class SplitExecutionRunner extends CompositeExecutionRunner implements Runnable {
    private static final long SPLIT_FLOW_TIMEOUT_SECONDS = 300;
    private Split split;
    private CountDownLatch latch;

    public SplitExecutionRunner(SplitContextImpl splitContext, CompositeExecutionRunner enclosingRunner) {
        super(splitContext, enclosingRunner);
        this.split = splitContext.getSplit();
        this.batchContext = splitContext;
    }

    @Override
    protected List<?> getJobElements() {
        return split.getFlow();
    }

    @Override
    public void run() {
        List<Flow> flows = split.getFlow();
        latch = new CountDownLatch(flows.size());
        try {
            for (Flow f : flows) {
                runFlow(f, latch);
            }
            latch.await(SPLIT_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Throwable e) {
            LOGGER.failToRunJob(e, split, "run");
            for (AbstractContext c : batchContext.getOuterContexts()) {
                c.setBatchStatus(JobOperator.BatchStatus.FAILED);
            }
        }

        String next = split.getNext();
        if (next == null) {
            next = resolveControlElements(split.getControlElements());
        }
        enclosingRunner.runJobElement(next);
    }

}
