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

package org.jberet.runtime.runner;

import java.util.List;
import java.util.regex.Pattern;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.End;
import org.jberet.job.Fail;
import org.jberet.job.Next;
import org.jberet.job.Stop;
import org.jberet.runtime.context.AbstractContext;

public abstract class AbstractRunner<C extends AbstractContext> {
    /**
     * The id of the job element this runner represents.
     */
    protected String id;
    protected C batchContext;
    protected CompositeExecutionRunner enclosingRunner;

    protected AbstractRunner(C batchContext, CompositeExecutionRunner enclosingRunner) {
        this.id = batchContext.getId();
        this.batchContext = batchContext;
        this.enclosingRunner = enclosingRunner;
    }

    protected static final boolean matches(String text, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        boolean containsQuestionMark = pattern.contains("?");
        if (containsQuestionMark) {
            pattern = pattern.replace('?', '.');
        }
        boolean containsAsterisk = pattern.contains("*");
        if (containsAsterisk) {
            pattern = pattern.replace("*", ".*");
        }
        if (!containsAsterisk && !containsQuestionMark) {
            return text.equals(pattern);
        }
        return Pattern.matches(pattern, text);
    }

    /**
     * Resolves a list of next, end, stop and fail elements to determine the next job element.
     *
     * @param transitionElements the group of control elements, i.e., next, end, stop and fail
     * @param nextAttr the next attribute value
     * @param partOfDecision if these transition elements are part of a decision element.  If so the current
     *                       batchContext's status will be updated to the terminating status.  Otherwise, these
     *                       transition elements are part of a step or flow, and the terminating status has no
     *                       bearing on the current batchContext.
     * @return the ref name of the next execution element
     */
    protected String resolveTransitionElements(List<?> transitionElements, String nextAttr, boolean partOfDecision) {
        String exitStatus = batchContext.getExitStatus();
        for (Object e : transitionElements) {  //end, fail. next, stop
            if (e instanceof Next) {
                Next next = (Next) e;
                if (matches(exitStatus, next.getOn())) {
                    return next.getTo();
                }
            } else if (e instanceof End) {
                End end = (End) e;
                if (matches(exitStatus, end.getOn())) {
                    setOuterContextStatus(batchContext.getOuterContexts(), BatchStatus.COMPLETED,
                            exitStatus, end.getExitStatus(), partOfDecision);
                    return null;
                }
            } else if (e instanceof Fail) {
                Fail fail = (Fail) e;
                if (matches(exitStatus, fail.getOn())) {
                    setOuterContextStatus(batchContext.getOuterContexts(), BatchStatus.FAILED,
                            exitStatus, fail.getExitStatus(), partOfDecision);
                    return null;
                }
            } else {  //stop
                Stop stop = (Stop) e;
                if (matches(exitStatus, stop.getOn())) {
                    setOuterContextStatus(batchContext.getOuterContexts(), BatchStatus.STOPPED,
                            exitStatus, stop.getExitStatus(), partOfDecision);
                    String restartPoint = stop.getRestart();  //job-level step, flow or split to restart
                    if (restartPoint != null) {
                        batchContext.getJobContext().getJobExecution().setRestartPoint(restartPoint);
                    }
                    return null;
                }
            }
        }
        return nextAttr;
    }

    private void setOuterContextStatus(AbstractContext[] outerContexts, BatchStatus batchStatus,
                                       String currentExitStatus, String newExitStatus, boolean partOfDecision) {
        String exitStatusToUse;
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

        for (AbstractContext c : outerContexts) {
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
