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
import javax.batch.api.Decider;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.StepExecution;

import org.mybatch.job.Decision;
import org.mybatch.job.Flow;
import org.mybatch.job.Split;
import org.mybatch.job.Step;
import org.mybatch.runtime.context.AbstractContext;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchLogger;

/**
 * A runner for job elements that can contain other job elements.  Examples of such composite job elements are
 * job, flow and split.
 */
public abstract class CompositeExecutionRunner extends AbstractRunner {
    protected List<?> jobElements;

    protected CompositeExecutionRunner(AbstractContext batchContext, CompositeExecutionRunner enclosingRunner, List<?> jobElements) {
        super(batchContext, enclosingRunner);
        this.jobElements = jobElements;
    }

    /**
     * Runs the job element including step, decision, flow, and split.
     *
     * @param jobElementName          ref name of the job element
     * @param precedingStepExecutions 0 or 1 StepExecution, 1 StepExecution is passed in for decision element, and 0 StepExecution for others.
     */
    protected void runJobElement(String jobElementName, StepExecution... precedingStepExecutions) {
        if (jobElementName == null) {
            return;
        }
        for (Object e : jobElements) {
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

        BatchLogger.LOGGER.unrecognizableJobElement(jobElementName, id);
    }

    protected void runStep(Step step) {
        StepContextImpl stepContext = new StepContextImpl(step,
                AbstractContext.addToContextArray(batchContext.getOuterContexts(), batchContext));
        StepExecutionRunner stepExecutionRunner = new StepExecutionRunner(stepContext, this);
        stepExecutionRunner.run();
    }

    protected void runDecision(Decision decision, StepExecution... precedingStepExecutions) {
        Decider decider = (Decider) batchContext.getJobContext().createArtifact(decision.getRef(), decision.getProperties());
        String newExitStatus;
        try {
            newExitStatus = precedingStepExecutions.length == 1 ?
                    decider.decide(precedingStepExecutions[0]) : decider.decide(precedingStepExecutions);
            batchContext.setExitStatus(newExitStatus);
            String next = resolveControlElements(decision.getControlElements());
            runJobElement(next, precedingStepExecutions);
        } catch (Exception e) {
            BatchLogger.LOGGER.failToRunJob(e, decider, "decide");
            batchContext.setBatchStatus(JobOperator.BatchStatus.FAILED);
            return;
        }
    }

    protected void runFlow(Flow flow) {

    }

    protected void runSplit(Split split) {

    }
}
