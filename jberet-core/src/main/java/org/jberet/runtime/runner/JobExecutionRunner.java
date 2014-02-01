/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.runner;

import java.util.List;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.BatchStatus;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.runtime.context.JobContextImpl;

public final class JobExecutionRunner extends CompositeExecutionRunner<JobContextImpl> implements Runnable {
    private final Job job;

    public JobExecutionRunner(final JobContextImpl jobContext) {
        super(jobContext, null);
        this.job = jobContext.getJob();
    }

    @Override
    protected List<? extends JobElement> getJobElements() {
        return job.getJobElements();
    }

    @Override
    public void run() {
        // the job may be stopped right after starting
        if (batchContext.getBatchStatus() != BatchStatus.STOPPING) {
            batchContext.setBatchStatus(BatchStatus.STARTED);
        }
        try {
            // run job listeners beforeJob()
            for (final JobListener l : batchContext.getJobListeners()) {
                try {
                    l.beforeJob();
                } catch (final Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", l);
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    return;
                    //TODO: fix the flow, if beforeJob fails, shoud afterJob still run?
                }
            }

            runFromHeadOrRestartPoint(batchContext.getJobExecution().getRestartPosition());

            for (final JobListener l : batchContext.getJobListeners()) {
                try {
                    l.afterJob();
                } catch (final Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", l);
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    return;
                }
            }
        } catch (final Throwable e) {
            BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", job);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        }

        batchContext.destroyArtifact(batchContext.getJobListeners());

        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
            batchContext.setBatchStatus(BatchStatus.COMPLETED);
        } else if (batchContext.getBatchStatus() == BatchStatus.STOPPING) {
            batchContext.setBatchStatus(BatchStatus.STOPPED);
        }
        batchContext.getJobRepository().updateJobExecution(batchContext.getJobExecution());
        batchContext.getJobExecution().cleanUp();
    }
}
