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
import java.util.regex.Pattern;
import javax.batch.runtime.BatchStatus;

import org.mybatch.job.End;
import org.mybatch.job.Fail;
import org.mybatch.job.Next;
import org.mybatch.job.Stop;
import org.mybatch.runtime.context.AbstractContext;

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

    public CompositeExecutionRunner getEnclosingRunner() {
        return enclosingRunner;
    }

    public C getBatchContext() {
        return this.batchContext;
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
     * @return the ref name of the next execution element
     */
    protected String resolveTransitionElements(List<?> transitionElements) {
        String result = null;
        String exitStatus = batchContext.getExitStatus();
        String newExitStatus = null;  //optional, may be null

        for (Object e : transitionElements) {  //end, fail. next, stop
            if (e instanceof Next) {
                Next next = (Next) e;
                if (matches(exitStatus, next.getOn())) {
                    return next.getTo();
                }
            } else if (e instanceof End) {
                End end = (End) e;
                if (matches(exitStatus, end.getOn())) {
                    batchContext.setBatchStatus(BatchStatus.COMPLETED);
                    newExitStatus = end.getExitStatus();
                    if (newExitStatus != null) {
                        batchContext.setExitStatus(newExitStatus);
                    }
                    for (AbstractContext c : batchContext.getOuterContexts()) {
                        c.setBatchStatus(BatchStatus.COMPLETED);
                        c.setExitStatus(newExitStatus == null ? exitStatus : newExitStatus);
                    }
                    return null;
                }
            } else if (e instanceof Fail) {
                Fail fail = (Fail) e;
                if (matches(exitStatus, fail.getOn())) {
                    batchContext.setBatchStatus(BatchStatus.FAILED);
                    newExitStatus = fail.getExitStatus();
                    if (newExitStatus != null) {
                        batchContext.setExitStatus(newExitStatus);
                    }
                    for (AbstractContext c : batchContext.getOuterContexts()) {
                        c.setBatchStatus(BatchStatus.FAILED);
                        c.setExitStatus(newExitStatus == null ? exitStatus : newExitStatus);
                    }
                    return null;
                }
            } else {  //stop
                Stop stop = (Stop) e;
                if (matches(exitStatus, stop.getOn())) {
                    batchContext.setBatchStatus(BatchStatus.STOPPED);
                    newExitStatus = stop.getExitStatus();
                    if (newExitStatus != null) {
                        batchContext.setExitStatus(newExitStatus);
                    }
                    for (AbstractContext c : batchContext.getOuterContexts()) {
                        c.setBatchStatus(BatchStatus.STOPPED);
                        c.setExitStatus(newExitStatus == null ? exitStatus : newExitStatus);
                    }
                    String restartPoint = stop.getRestart();  //job-level step, flow or split to restart
                    if (restartPoint != null) {
                        batchContext.getJobContext().getJobExecution().setRestartPoint(restartPoint);
                    }
                    return null;
                }
            }
        }
        return result;
    }


}
