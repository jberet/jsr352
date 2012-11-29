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

import java.util.Map;
import java.util.Set;
import javax.batch.state.StepExecution;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Step;
import org.mybatch.metadata.ApplicationMetaData;

public class StepExecutionRunner implements Runnable {
    private Step step;
    private StepExecution stepExecution;
    private JobExecutionRunner jobExecutionRunner;

    public StepExecutionRunner(Step step, StepExecution stepExecution, JobExecutionRunner jobExecutionRunner) {
        this.step = step;
        this.stepExecution = stepExecution;
        this.jobExecutionRunner = jobExecutionRunner;
    }

    @Override
    public void run() {
        Chunk chunk = step.getChunk();
        Batchlet batchlet = step.getBatchlet();
        if(chunk != null && batchlet != null) {
            throw new IllegalStateException("A step cannot contain both Chunk type step and batchlet type step.");
        }

        ApplicationMetaData appData = jobExecutionRunner.getJobInstance().getApplicationMetaData();
        Map<String, Set<String>> annotationIndex = appData.getAnnotationIndex();
        Set<String> batchletClasses = annotationIndex.get("javax.batch.annotation.Batchlet");
    }
}
