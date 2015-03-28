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
import java.util.regex.Pattern;
import javax.batch.runtime.BatchStatus;

import org.jberet._private.BatchMessages;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Script;
import org.jberet.job.model.Transition.End;
import org.jberet.job.model.Transition.Fail;
import org.jberet.job.model.Transition.Next;
import org.jberet.job.model.Transition.Stop;
import org.jberet.job.model.XmlAttribute;
import org.jberet.runtime.context.AbstractContext;
import org.jberet.runtime.context.FlowContextImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;

public abstract class AbstractRunner<C extends AbstractContext> implements Runnable {
    /**
     * The id of the job element this runner represents.
     */
    protected String id;
    protected C batchContext;
    protected JobContextImpl jobContext;
    protected CompositeExecutionRunner enclosingRunner;

    protected AbstractRunner(final C batchContext, final CompositeExecutionRunner enclosingRunner) {
        this.id = batchContext.getId();
        this.batchContext = batchContext;
        this.jobContext = batchContext.getJobContext();
        this.enclosingRunner = enclosingRunner;
    }

    protected static final boolean matches(final String text, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        final boolean containsQuestionMark = pattern.contains("?");
        if (containsQuestionMark) {
            pattern = pattern.replace('?', '.');
        }
        final boolean containsAsterisk = pattern.contains("*");
        if (containsAsterisk) {
            pattern = pattern.replace("*", ".*");
        }
        if (!containsAsterisk && !containsQuestionMark) {
            return pattern.equals(text);
        }
        return Pattern.matches(pattern, text);
    }

    /**
     * Resolves a list of next, end, stop and fail elements to determine the next job element.
     *
     * @param transitionElements the group of control elements, i.e., next, end, stop and fail
     * @param nextAttr           the next attribute value
     * @param partOfDecision     if these transition elements are part of a decision element.  If so the current
     *                           batchContext's status will be updated to the terminating status.  Otherwise, these
     *                           transition elements are part of a step or flow, and the terminating status has no
     *                           bearing on the current batchContext.
     * @return the ref name of the next execution element
     */
    protected String resolveTransitionElements(final List<?> transitionElements, final String nextAttr, final boolean partOfDecision) {
        final String exitStatus = batchContext.getExitStatus();
        for (final Object e : transitionElements) {  //end, fail. next, stop
            if (e instanceof Next) {
                final Next next = (Next) e;
                if (matches(exitStatus, next.getOn())) {
                    return next.getTo();
                }
            } else if (e instanceof End) {
                final End end = (End) e;
                if (matches(exitStatus, end.getOn())) {
                    final AbstractContext[] outerContexts = batchContext.getOuterContexts();
                    for (final AbstractContext abc : outerContexts) {
                        if (abc instanceof FlowContextImpl) {
                            ((FlowContextImpl) abc).getFlowExecution().setEnded(true);
                        }
                    }
                    setOuterContextStatus(outerContexts, BatchStatus.COMPLETED,
                            exitStatus, end.getExitStatus(), partOfDecision);
                    return null;
                }
            } else if (e instanceof Fail) {
                final Fail fail = (Fail) e;
                if (matches(exitStatus, fail.getOn())) {
                    setOuterContextStatus(batchContext.getOuterContexts(), BatchStatus.FAILED,
                            exitStatus, fail.getExitStatus(), partOfDecision);
                    return null;
                }
            } else {  //stop
                final Stop stop = (Stop) e;
                if (matches(exitStatus, stop.getOn())) {
                    setOuterContextStatus(batchContext.getOuterContexts(), BatchStatus.STOPPED,
                            exitStatus, stop.getExitStatus(), partOfDecision);
                    final String restartPoint = stop.getRestart();  //job-level step, flow or split to restart
                    if (restartPoint != null) {
                        batchContext.getJobContext().getJobExecution().setRestartPosition(restartPoint);
                    }
                    return null;
                }
            }
        }
        return nextAttr;
    }

    /**
     * Creates batch artifact from job xml configuration element, e.g, batchlet, reader, processor, writer, etc.
     * These element may contain a ref attribute indicating the reference name for the artifact, or in absense of ref
     * attribute, a script sub-element specifies the script to run instead.
     *
     * @param refArtifact the job xml configuration element for the artifact to create
     * @param stepContext the current StepContextImpl
     * @param aClass the artifact class: org.jberet.runtime.runner.ScriptBatchlet,
     *                      org.jberet.runtime.runner.ScriptItemReader,
     *                      org.jberet.runtime.runner.ScriptItemWriter, or
     *                      org.jberet.runtime.runner.ScriptItemProcessor
     * @return the created artifact object
     * @throws Exception
     */
    Object createArtifact(final RefArtifact refArtifact, final StepContextImpl stepContext, final Class<?> aClass) throws Exception {
        final String ref = refArtifact.getRef();
        if (ref.isEmpty()) {
            final Script script = refArtifact.getScript();
            if (script == null) {
                throw BatchMessages.MESSAGES.failToGetAttribute(XmlAttribute.REF.getLocalName(), null);
            }
            return aClass == ScriptItemReader.class ? new ScriptItemReader(script, refArtifact.getProperties(), stepContext) :
                   aClass == ScriptItemWriter.class ? new ScriptItemWriter(script, refArtifact.getProperties(), stepContext) :
                   aClass == ScriptItemProcessor.class ? new ScriptItemProcessor(script, refArtifact.getProperties(), stepContext) :
                   new ScriptBatchlet(script, refArtifact.getProperties(), stepContext);
        } else {
            return jobContext.createArtifact(ref, null, refArtifact.getProperties(), stepContext);
        }
    }

    private void setOuterContextStatus(final AbstractContext[] outerContexts, final BatchStatus batchStatus,
                                       final String currentExitStatus, final String newExitStatus, final boolean partOfDecision) {
        final String exitStatusToUse;
        //for decision, the currentExitStatus is from the decider class
        if (partOfDecision) {
            exitStatusToUse = newExitStatus != null ? newExitStatus : currentExitStatus;
        } else {
            exitStatusToUse = newExitStatus;
        }

        //if these elements are part of a step, or flow, the new batch status and exit status do not affect
        //the already-completed step or flow.  If part of a decision, then yes.
        if (partOfDecision) {
            batchContext.setBatchStatus(batchStatus);
            batchContext.setExitStatus(exitStatusToUse);
        }

        for (final AbstractContext c : outerContexts) {
            c.setBatchStatus(batchStatus);

            //inside this method are all terminating transition elements, and
            // the exitStatus returned from a decider should be applied to the entire job
            if (partOfDecision) {
                c.setExitStatus(exitStatusToUse);
            } else if (exitStatusToUse != null) {
                c.setExitStatus(exitStatusToUse);
            }
        }
    }

}
