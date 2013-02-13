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

package org.mybatch.runtime;

import java.sql.Timestamp;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.mybatch.job.Step;
import org.mybatch.runtime.context.StepContextImpl;

public class StepExecutionImpl<P> implements StepExecution<P> {
    private long id;

    private Step step;

    private StepContextImpl stepContext;

    private JobExecutionImpl jobExecution;

    private P persistentData;

    public StepContextImpl getStepContext() {
        return stepContext;
    }

    public void setStepContext(StepContextImpl stepContext) {
        this.stepContext = stepContext;
    }

    public StepExecutionImpl(Step step, JobExecutionImpl jobExecution) {
        //TODO initialize id
        this.step = step;
        this.jobExecution = jobExecution;
    }

    public long getId() {
        return this.id;
    }

    @Override
    public long getJobExecutionId() {
        return jobExecution.getExecutionId();
    }

    @Override
    public String getStatus() {
        return stepContext.getBatchStatus();
    }

    @Override
    public Timestamp getStartTime() {
        return null;
    }

    @Override
    public Timestamp getEndTime() {
        return null;
    }

    @Override
    public String getExitStatus() {
        return stepContext.getExitStatus();
    }

    @Override
    public P getUserPersistentData() {
        return persistentData;
    }

    @Override
    public Metric[] getMetrics() {
        return stepContext.getMetrics();
    }

}
