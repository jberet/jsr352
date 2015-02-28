/*
 * Copyright (c) 2012-2015 Red Hat, Inc. and/or its affiliates.
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
import org.jberet.runtime.JobExecutionImpl;
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
        final JobExecutionImpl jobExecution = batchContext.getJobExecution();

        // the job may be stopped right after starting
        if (jobExecution.getBatchStatus() != BatchStatus.STOPPING) {
            jobExecution.setBatchStatus(BatchStatus.STARTED);
            batchContext.getJobRepository().updateJobExecution(jobExecution, false);
        }
        final JobListener[] jobListeners = batchContext.getJobListeners();
        int i = 0;

        try {
            for (; i < jobListeners.length; i++) {
                jobListeners[i].beforeJob();
            }
            runFromHeadOrRestartPoint(jobExecution.getRestartPosition());
        } catch (final Throwable e) {
            BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", job);
            jobExecution.setBatchStatus(BatchStatus.FAILED);
        } finally {
            for (i = 0; i < jobListeners.length; i++) {
                try {
                    jobListeners[i].afterJob();
                } catch (final Throwable e) {
                    BatchLogger.LOGGER.failToRunJob(e, job.getId(), "", jobListeners[i]);
                    jobExecution.setBatchStatus(BatchStatus.FAILED);
                }
            }
        }

        batchContext.destroyArtifact(jobListeners);

        final BatchStatus batchStatus = jobExecution.getBatchStatus();
        if (batchStatus == BatchStatus.STARTED) {
            jobExecution.setBatchStatus(BatchStatus.COMPLETED);
        } else if (batchStatus == BatchStatus.STOPPING) {
            jobExecution.setBatchStatus(BatchStatus.STOPPED);
            adjustRestart(jobExecution);
        } else if (batchStatus == BatchStatus.FAILED) {
            adjustRestart(jobExecution);
        } else if (batchStatus == BatchStatus.STOPPED) {
            adjustRestart(jobExecution);
        }

        batchContext.getJobRepository().updateJobExecution(jobExecution, true);
        jobExecution.cleanUp();
    }

    private void adjustRestart(final JobExecutionImpl jobExecution) {
        if (!job.getRestartableBoolean()) {
            jobExecution.setRestartPosition(Job.UNRESTARTABLE);
        }
    }
}
