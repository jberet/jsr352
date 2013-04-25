/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jberet.runtime.runner;

import java.util.List;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.Job;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.util.BatchLogger;

public final class JobExecutionRunner extends CompositeExecutionRunner<JobContextImpl> implements Runnable {
    private Job job;

    public JobExecutionRunner(JobContextImpl jobContext) {
        super(jobContext, null);
        this.job = jobContext.getJob();
    }

    @Override
    protected List<?> getJobElements() {
        return job.getDecisionOrFlowOrSplit();
    }

    @Override
    public void run() {
        batchContext.setBatchStatus(BatchStatus.STARTED);
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
        } finally {
            batchContext.getJobExecution().setSubstitutedJob(null);
        }

        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
            batchContext.setBatchStatus(BatchStatus.COMPLETED);
        } else if (batchContext.getBatchStatus() == BatchStatus.STOPPING) {
            batchContext.setBatchStatus(BatchStatus.STOPPED);
        }

        batchContext.saveInactiveStepExecutions();
    }

}
