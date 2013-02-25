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
import javax.batch.api.Decider;
import javax.batch.api.JobListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.StepExecution;

import org.mybatch.job.Decision;
import org.mybatch.job.Flow;
import org.mybatch.job.Job;
import org.mybatch.job.Split;
import org.mybatch.job.Step;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.JobInstanceImpl;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.runtime.context.JobContextImpl;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchLogger;

public final class JobExecutionRunner extends AbstractRunner implements Runnable {
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

    /**
     * Runs the job element including step, decision, flow, and split.
     * @param jobElementName ref name of the job element
     * @param precedingStepExecutions 0 or 1 StepExecution, 1 StepExecution is passed in for decision element, and 0 StepExecution for others.
     */
    protected void runJobElement(String jobElementName, StepExecution... precedingStepExecutions) {
        if (jobElementName == null) {
            return;
        }
        for (Serializable e : job.getDecisionOrFlowOrSplit()) {
            if (e instanceof Step) {
                Step step = (Step) e;
                if (step.getId().equals(jobElementName)) {
                    runStep(step);
                    return;
                }
            } else if (e instanceof Decision) {
                Decision decision = (Decision) e;
                if (decision.getId().equals(jobElementName)) {
                    runDecision(decision, precedingStepExecutions);
                    return;
                }
            } else if (e instanceof Flow) {
                Flow flow = (Flow) e;
                if (flow.getId().equals(jobElementName)) {
                    runFlow(flow);
                    return;
                }
            } else if (e instanceof Split) {
                Split split = (Split) e;
                if (split.getId().equals(jobElementName)) {
                    runSplit(split);
                    return;
                }
            }
        }

        BatchLogger.LOGGER.unrecognizableJobElement(jobElementName, job.getId());
    }

    protected void runStep(Step step) {
        StepExecutionImpl stepExecution = new StepExecutionImpl(step, jobExecution);
        StepContextImpl stepContext = new StepContextImpl(step, stepExecution.getId(), jobContext);
        stepExecution.setStepContext(stepContext);

        StepExecutionRunner stepExecutionRunner = new StepExecutionRunner(step, stepExecution, stepContext, this);
        stepExecutionRunner.run();
    }

    protected void runDecision(Decision decision, StepExecution... precedingStepExecutions) {
        Decider decider = (Decider) jobContext.createArtifact(decision.getRef(), decision.getProperties());
        String newExitStatus;
        try {
            newExitStatus = precedingStepExecutions.length == 1 ?
                    decider.decide(precedingStepExecutions[0]) : decider.decide(precedingStepExecutions);
            jobContext.setExitStatus(newExitStatus);
            String next = resolveControlElements(decision.getControlElements(), jobContext);
            runJobElement(next, precedingStepExecutions);
        } catch (Exception e) {
            BatchLogger.LOGGER.failToRunJob(e, decider, "decide");
            jobContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
            return;
        }

    }

    protected void runFlow(Flow flow) {

    }

    protected void runSplit(Split split) {

    }

}
