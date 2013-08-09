/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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
import java.util.concurrent.CountDownLatch;
import javax.batch.api.Decider;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.FlowContextImpl;
import org.jberet.runtime.context.SplitContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.util.BatchLogger;
import org.jberet.util.ConcurrencyService;

/**
 * A runner for job elements that can contain other job elements.  Examples of such composite job elements are
 * job, flow and split.
 */
public abstract class CompositeExecutionRunner<C extends AbstractContext> extends AbstractRunner<C> {
    protected CompositeExecutionRunner(C batchContext, CompositeExecutionRunner enclosingRunner) {
        super(batchContext, enclosingRunner);
    }

    protected abstract List<? extends JobElement> getJobElements();

    /**
     * Runs the first job element, which then transitions to the next element.  Not used for running split, whose
     * component elements are not sequential.
     */
    protected void runFromHeadOrRestartPoint(String restartPoint) {
        if (restartPoint != null) {
            //clear the restart point passed over from original job execution.  This execution may have its own
            //restart point or null (start from head) for use by the next restart.
            jobContext.getJobExecution().setRestartPoint(null);
            for (JobElement e : getJobElements()) {
                if (e instanceof Step) {
                    Step step = (Step) e;
                    if (step.getId().equals(restartPoint)) {
                        runStep(step);
                        break;
                    }
                } else if (e instanceof Flow) {
                    Flow flow = (Flow) e;
                    if (flow.getId().equals(restartPoint)) {
                        runFlow(flow, null);
                        break;
                    }
                } else if (e instanceof Split) {
                    Split split = (Split) e;
                    if (split.getId().equals(restartPoint)) {
                        runSplit(split);
                        break;
                    }
                } else if (e instanceof Decision) {
                    Decision decision = (Decision) e;
                    if (decision.getId().equals(restartPoint)) {
                        runDecision(decision);
                    }
                    break;
                }
            }
        } else {
            // the head of the composite job element is the first non-abstract element (step, flow, or split)
            for (JobElement e : getJobElements()) {
                if (e instanceof Step) {
                    Step step = (Step) e;
//                if (Boolean.parseBoolean(step.getAbstract())) {
//                    continue;
//                }
                    runStep(step);
                    break;
                } else if (e instanceof Flow) {
                    Flow flow = (Flow) e;
                    //A flow cannot be abstract or have parent, so run the flow
                    runFlow(flow, null);
                    break;
                } else if (e instanceof Split) {
                    Split split = (Split) e;
                    //A split cannot be abstract or have parent, so run the split
                    runSplit(split);
                    break;
                } else if (e instanceof Decision) {
                    Decision decision = (Decision) e;
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    BatchLogger.LOGGER.decisionCannotBeFirst(decision.getId());
                    return;

//                runDecision(decision);
                }
            }
        }
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
        for (JobElement e : getJobElements()) {
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
                    runFlow(flow, null);
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

        throw BatchLogger.LOGGER.unrecognizableJobElement(jobElementName, id);
    }

    protected void runStep(Step step) {
        StepContextImpl stepContext = new StepContextImpl(step,
                AbstractContext.addToContextArray(batchContext.getOuterContexts(), batchContext));
        StepExecutionRunner stepExecutionRunner = new StepExecutionRunner(stepContext, this);

        if (batchContext instanceof FlowContextImpl) {
            ((FlowContextImpl) batchContext).getFlowExecution().setLastStepExecution(stepContext.getStepExecution());
        }

        stepExecutionRunner.run();
    }

    protected void runDecision(Decision decision, StepExecution... precedingStepExecutions) {
        Decider decider = jobContext.createArtifact(decision.getRef(), null, decision.getProperties());
        String newExitStatus;
        try {
            newExitStatus = decider.decide(precedingStepExecutions);
            batchContext.setExitStatus(newExitStatus);
            String next = resolveTransitionElements(decision.getTransitionElements(), null, true);
            runJobElement(next, precedingStepExecutions);
        } catch (Exception e) {
            BatchLogger.LOGGER.failToRunJob(e, jobContext.getJobName(), decision.getRef(), decider);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        } finally {
            jobContext.destroyArtifact(decider);
        }
    }

    protected void runFlow(Flow flow, CountDownLatch latch) {
        FlowContextImpl flowContext;
        AbstractContext[] outerContextsToUse = AbstractContext.addToContextArray(batchContext.getOuterContexts(), batchContext);
        if (batchContext instanceof SplitContextImpl) {
            SplitContextImpl splitContext = (SplitContextImpl) batchContext;
            outerContextsToUse[0] = splitContext.getJobContext().clone();
            flowContext = new FlowContextImpl(flow, outerContextsToUse);
            splitContext.getFlowExecutions().add(flowContext.getFlowExecution());
        } else {
            flowContext = new FlowContextImpl(flow, outerContextsToUse);
        }

        FlowExecutionRunner flowExecutionRunner = new FlowExecutionRunner(flowContext, this, latch);

        if (latch != null) {
            ConcurrencyService.getExecutorService().submit(flowExecutionRunner);
        } else {
            flowExecutionRunner.run();
        }
    }

    protected void runSplit(Split split) {
        SplitContextImpl splitContext = new SplitContextImpl(split,
                AbstractContext.addToContextArray(batchContext.getOuterContexts(), batchContext));
        SplitExecutionRunner splitExecutionRunner = new SplitExecutionRunner(splitContext, this);
        splitExecutionRunner.run();
    }
}
