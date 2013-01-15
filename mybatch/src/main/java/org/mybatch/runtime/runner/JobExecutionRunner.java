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

import org.mybatch.job.Job;
import org.mybatch.job.Step;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.JobInstanceImpl;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.runtime.context.JobContextImpl;
import org.mybatch.util.BatchUtil;

public class JobExecutionRunner implements Runnable {
    private Job job;
    private JobInstanceImpl jobInstance;
    private JobExecutionImpl jobExecution;

    public JobExecutionRunner(Job job, JobInstanceImpl jobInstance, JobExecutionImpl jobExecution) {
        this.job = job;
        this.jobInstance = jobInstance;
        this.jobExecution = jobExecution;

        JobContextImpl jobContext = new JobContextImpl(job.getId(), jobInstance.getInstanceId(),
                jobExecution.getExecutionId(), BatchUtil.getPropertiesFromJobDefinition(job));

        jobExecution.setJobContext(jobContext);
    }

    public JobInstanceImpl getJobInstance() {
        return jobInstance;
    }

    public JobExecutionImpl getJobExecution() {
        return jobExecution;
    }

    @Override
    public void run() {
        // is the first element the beginning of the job?
        // need to collect into an ordered collection first.
        for(Serializable e : job.getDecisionOrFlowOrSplit()) {
            if(e instanceof Step) {
                Step step = (Step) e;
                StepExecutionImpl stepExecution = new StepExecutionImpl(step);
                StepExecutionRunner stepExecutionRunner = new StepExecutionRunner(step, stepExecution, this);
                stepExecutionRunner.run();
            }
        }
    }
}
