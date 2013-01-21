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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import javax.batch.runtime.StepExecution;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Step;
import org.mybatch.operations.JobOperatorImpl;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchUtil;
import org.mybatch.util.ConcurrencyService;

import static org.mybatch.util.BatchLogger.LOGGER;

public class StepExecutionRunner implements Runnable {
    private Step step;
    private StepExecutionImpl stepExecution;
    private StepContextImpl stepContext;
    private JobExecutionRunner jobExecutionRunner;
    private Future<?> stepResult;

    public static List<Class<? extends Annotation>> methodAnnotations = Arrays.asList(
            javax.batch.annotation.BeginStep.class,
            javax.batch.annotation.Process.class,
            javax.batch.annotation.EndStep.class
            //need to consider cancel @Stop
    );

    public StepExecutionRunner(Step step, StepExecutionImpl stepExecution, JobExecutionRunner jobExecutionRunner) {
        this.step = step;
        this.stepExecution = stepExecution;
        this.jobExecutionRunner = jobExecutionRunner;

        this.stepContext = new StepContextImpl(step.getId(), stepExecution.getJobExecutionId(),
                jobExecutionRunner.getJobExecution().getJobContext(),
                BatchUtil.getPropertiesFromStepDefinition(step));
        stepExecution.setStepContext(stepContext);
    }

    public StepContextImpl getStepContext() {
        return stepContext;
    }

    public JobExecutionRunner getJobExecutionRunner() {
        return jobExecutionRunner;
    }

    @Override
    public void run() {
        Chunk chunk = step.getChunk();
        Batchlet batchlet = step.getBatchlet();
        if (chunk != null && batchlet != null) {
            stepContext.setBatchStatus(JobOperatorImpl.BatchStatus.ABANDONED.name());
            LOGGER.cannotContainBothChunkAndBatchlet(step.getId());
            return;
        }

        BatchletRunner batchletRunner = new BatchletRunner(batchlet, this);
        stepResult = ConcurrencyService.submit(batchletRunner);
        LOGGER.submittedBatchletTask(batchlet.getRef(), Thread.currentThread());

        //TODO handle chunk type step

    }

    public static void invokeFunctionMethods(Object artifact, List<Class<? extends Annotation>> methodAnnotations)
            throws InvocationTargetException, IllegalAccessException {
        for(Class<? extends Annotation> ann : methodAnnotations) {
            Method m = StepExecutionRunner.getAnnotatedMethod(artifact, ann);
            if(m != null) {
                m.invoke(artifact);
            } else {
                throw LOGGER.noMethodMatchingAnnotation(ann, artifact);
            }
        }
    }

    public static Method getAnnotatedMethod(Object artifact, Class<? extends Annotation> annotationClass) {
        for(Method m : artifact.getClass().getDeclaredMethods()) {
            if (m.getAnnotation(annotationClass) != null) {
                return m;
            }
        }
        return null;
    }
}
