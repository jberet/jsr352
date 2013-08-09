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

import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.util.BatchLogger;

public final class JobExecutionRunner extends CompositeExecutionRunner<JobContextImpl> implements Runnable {
    private Job job;

    public JobExecutionRunner(JobContextImpl jobContext) {
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
            for (JobListener l : batchContext.getJobListeners()) {
                try {
                    l.beforeJob();
                } catch (Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", l);
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    return;
                }
            }

            runFromHeadOrRestartPoint(batchContext.getJobExecution().getRestartPoint());

            for (JobListener l : batchContext.getJobListeners()) {
                try {
                    l.afterJob();
                } catch (Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", l);
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    return;
                }
            }
        } catch (Throwable e) {
            BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", job);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        }

        batchContext.destroyArtifact(batchContext.getJobListeners());

        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
            batchContext.setBatchStatus(BatchStatus.COMPLETED);
        } else if (batchContext.getBatchStatus() == BatchStatus.STOPPING) {
            batchContext.setBatchStatus(BatchStatus.STOPPED);
        }
        batchContext.saveInactiveStepExecutions();
        batchContext.getJobExecution().cleanUp();
    }
}
