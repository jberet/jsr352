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

import org.mybatch.job.Flow;
import org.mybatch.job.Job;
import org.mybatch.job.Split;
import org.mybatch.job.Step;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.JobInstanceImpl;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.runtime.context.JobContextImpl;
import org.mybatch.runtime.context.StepContextImpl;

public class JobExecutionRunner implements Runnable {
    private Job job;
    private JobInstanceImpl jobInstance;
    private JobExecutionImpl jobExecution;
    private JobContextImpl jobContext;

    public JobExecutionRunner(JobInstanceImpl jobInstance, JobExecutionImpl jobExecution, JobContextImpl jobContext) {
        this.job = jobContext.getJob();
        this.jobContext = jobContext;
        this.jobInstance = jobInstance;
        this.jobExecution = jobExecution;
    }

    public JobInstanceImpl getJobInstance() {
        return jobInstance;
    }

    public JobExecutionImpl getJobExecution() {
        return jobExecution;
    }

    @Override
    public void run() {
        // the head of the job is the first non-abstract element
        for (Serializable e : job.getDecisionOrFlowOrSplit()) {
            if (e instanceof Step) {
                Step step = (Step) e;
                if (Boolean.parseBoolean(step.getAbstract())) {
                    continue;
                }
                StepExecutionImpl stepExecution = new StepExecutionImpl(step, jobExecution);
                StepContextImpl stepContext = new StepContextImpl(step, stepExecution.getId(), jobContext);
                stepExecution.setStepContext(stepContext);

                StepExecutionRunner stepExecutionRunner = new StepExecutionRunner(step, stepExecution, stepContext, this);
                stepExecutionRunner.run();
                break;
            } else if (e instanceof Flow) {
                Flow flow = (Flow) e;
                if (Boolean.parseBoolean(flow.getAbstract())) {
                    continue;
                }
                //run the flow
                break;
            } else if (e instanceof Split) {
                Split split = (Split) e;
                if (Boolean.parseBoolean(split.getAbstract())) {
                    continue;
                }
                //run the split
                break;
            }
        }
    }
}
