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

import java.util.regex.Pattern;
import javax.batch.api.StepListener;
import javax.batch.operations.JobOperator;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.End;
import org.mybatch.job.Fail;
import org.mybatch.job.Next;
import org.mybatch.job.Step;
import org.mybatch.job.Stop;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchLogger;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class StepExecutionRunner extends AbstractRunner implements Runnable {
    private Step step;
    private StepExecutionImpl stepExecution;
    private StepContextImpl stepContext;
    private JobExecutionRunner jobExecutionRunner;
    private Object stepResult;

    public StepExecutionRunner(Step step, StepExecutionImpl stepExecution, StepContextImpl stepContext, JobExecutionRunner jobExecutionRunner) {
        this.step = step;
        this.stepExecution = stepExecution;
        this.stepContext = stepContext;
        this.jobExecutionRunner = jobExecutionRunner;
    }

    public StepContextImpl getStepContext() {
        return stepContext;
    }

    public JobExecutionRunner getJobExecutionRunner() {
        return jobExecutionRunner;
    }

    @Override
    public void run() {
        stepContext.setBatchStatus(JobOperator.BatchStatus.STARTED.name());
        Chunk chunk = step.getChunk();
        Batchlet batchlet = step.getBatchlet();
        if (chunk == null && batchlet == null) {
            stepContext.setBatchStatus(JobOperator.BatchStatus.ABANDONED.name());
            LOGGER.stepContainsNoChunkOrBatchlet(step.getId());
            return;
        }

        if (chunk != null && batchlet != null) {
            stepContext.setBatchStatus(JobOperator.BatchStatus.ABANDONED.name());
            LOGGER.cannotContainBothChunkAndBatchlet(step.getId());
            return;
        }

        for (StepListener l : stepContext.getStepListeners()) {
            try {
                l.beforeStep();
            } catch (Throwable e) {
                BatchLogger.LOGGER.failToRunJob(e, l, "beforeStep");
                stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED.name());
                return;
            }
        }


        BatchletRunner batchletRunner = new BatchletRunner(batchlet, this);
        stepResult = batchletRunner.call();

        //TODO handle chunk type step


        for (StepListener l : stepContext.getStepListeners()) {
            try {
                l.afterStep();
            } catch (Throwable e) {
                BatchLogger.LOGGER.failToRunJob(e, l, "afterStep");
                stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED.name());
                return;
            }
        }

        if (stepContext.getBatchStatus() == JobOperator.BatchStatus.STARTED) {
            stepContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED.name());
        }

        if (stepContext.getBatchStatus() == JobOperator.BatchStatus.COMPLETED) {
            jobExecutionRunner.runJobElement(getNext());
        }
    }

    private String getNext() {
        String result = null;
        String nextAttr = step.getNext();
        if (nextAttr != null) {
            return nextAttr;
        }
        String exitStatus = stepContext.getExitStatus();
        for (Object e : step.getControlElements()) {  //end, fail. next, stop
            if (e instanceof Next) {
                Next next = (Next) e;
                if (matches(exitStatus, next.getOn())) {
                    return next.getTo();
                }
            } else if (e instanceof End) {
                End end = (End) e;
                if (matches(exitStatus, end.getOn())) {
                    stepContext.setBatchStatus(JobOperator.BatchStatus.COMPLETED.name());
                    stepContext.setExitStatus(end.getExitStatus());
                    return null;
                }
            } else if (e instanceof Fail) {
                Fail fail = (Fail) e;
                if (matches(exitStatus, fail.getOn())) {
                    stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED.name());
                    stepContext.setExitStatus(fail.getExitStatus());
                    return null;
                }
            } else {  //stop
                Stop stop = (Stop) e;
                if (matches(exitStatus, stop.getOn())) {
                    stepContext.setBatchStatus(JobOperator.BatchStatus.STOPPED.name());
                    stepContext.setExitStatus(stop.getExitStatus());
                    //TODO remember restart from stop.getRestart();
                    return null;
                }
            }
        }
        return result;
    }

    static final boolean matches(String text, String pattern) {
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
}
