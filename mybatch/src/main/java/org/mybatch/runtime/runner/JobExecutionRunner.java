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

package org.mybatch.runtime.runner;

import java.io.Serializable;
import javax.batch.api.JobListener;
import javax.batch.operations.JobOperator;

import org.mybatch.job.Flow;
import org.mybatch.job.Job;
import org.mybatch.job.Split;
import org.mybatch.job.Step;
import org.mybatch.runtime.context.JobContextImpl;
import org.mybatch.util.BatchLogger;

public final class JobExecutionRunner extends CompositeExecutionRunner implements Runnable {
    private Job job;
    private JobContextImpl jobContext;  //duplicate super.batchContext, for accessing StepContext-specific methods

    public JobExecutionRunner(JobContextImpl jobContext) {
        super(jobContext, null, jobContext.getJob().getDecisionOrFlowOrSplit());
        this.job = jobContext.getJob();
        this.jobContext = jobContext;
    }

    @Override
    public void run() {
        jobContext.setBatchStatus(JobOperator.BatchStatus.STARTED);
        // run job listeners beforeJob()
        for (JobListener l : jobContext.getJobListeners()) {
            try {
                l.beforeJob();
            } catch (Throwable e) {
                BatchLogger.LOGGER.failToRunJob(e, l, "beforeJob");
                jobContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                return;
            }
        }

        // the head of the job is the first non-abstract element (step, flow, or split)
        for (Serializable e : job.getDecisionOrFlowOrSplit()) {
            if (e instanceof Step) {
                Step step = (Step) e;
                if (Boolean.parseBoolean(step.getAbstract())) {
                    continue;
                }
                runStep(step);
                break;
            } else if (e instanceof Flow) {
                Flow flow = (Flow) e;
                //A flow cannot be abstract or have parent, so run the flow
                runFlow(flow);
                break;
            } else if (e instanceof Split) {
                Split split = (Split) e;
                //A split cannot be abstract or have parent, so run the split
                runSplit(split);
                break;
            }
        }

        for (JobListener l : jobContext.getJobListeners()) {
            try {
                l.afterJob();
            } catch (Throwable e) {
                BatchLogger.LOGGER.failToRunJob(e, l, "afterJob");
                jobContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
                return;
            }
        }
        if (jobContext.getBatchStatus() == JobOperator.BatchStatus.STARTED) {
            jobContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED);
        }
    }

}
