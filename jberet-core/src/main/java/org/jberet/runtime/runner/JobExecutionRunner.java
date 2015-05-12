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
            batchContext.getJobRepository().updateJobExecution(jobExecution, false, false);
        }
        final JobListener[] jobListeners = batchContext.getJobListeners();
        int i = 0;

        try {
            for (; i < jobListeners.length; i++) {
                jobListeners[i].beforeJob();
            }

            runFromHeadOrRestartPoint(jobExecution.getRestartPosition());

            if (jobExecution.getBatchStatus() == BatchStatus.STARTED) {
                jobExecution.setBatchStatus(BatchStatus.COMPLETED);
            }
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

        boolean saveJobParameters = false;
        switch (jobExecution.getBatchStatus()) {
            case COMPLETED:
                break;
            case STARTED:
                jobExecution.setBatchStatus(BatchStatus.COMPLETED);
                break;
            case STOPPING:
                jobExecution.setBatchStatus(BatchStatus.STOPPED);
                //fall through
            case FAILED:
            case STOPPED:
                saveJobParameters = adjustRestartFailedOrStopped(jobExecution);
                break;
        }

        batchContext.getJobRepository().updateJobExecution(jobExecution, true, saveJobParameters);
        batchContext.setTransientUserData(null);
        jobExecution.cleanUp();
    }

    /**
     * Adjusts restart position and job xml name if needed for FAILED or STOPPED job execution.
     *
     * @param jobExecution a failed or stopped job execution
     * @return true if the internal job parameter with key {@link org.jberet.job.model.Job#JOB_XML_NAME} was added to
     * {@code jobExecution}; false otherwise.
     */
    private boolean adjustRestartFailedOrStopped(final JobExecutionImpl jobExecution) {
        if (!job.getRestartableBoolean()) {
            jobExecution.setRestartPosition(Job.UNRESTARTABLE);
        }
        if (job.getJobXmlName() != null) {
            //jobXmlName is different than jobId, save it so the restart can correctly locate job xml file if the job
            //is not available from the cache.
            jobExecution.addJobParameter(Job.JOB_XML_NAME, job.getJobXmlName());
            return true;
        }
        return false;
    }
}
