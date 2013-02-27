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

import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.mybatch.runtime.metric.StepMetrics;

public final class StepExecutionImpl<P> extends AbstractExecution implements StepExecution<P> {
    private long id;

    private String stepId;

    private JobExecutionImpl rootJobExecution;

    private P userPersistentData;

    private StepMetrics stepMetrics = new StepMetrics();

    public StepExecutionImpl(JobExecutionImpl rootJobExecution, String stepId) {
        //TODO initialize id
        this.rootJobExecution = rootJobExecution;
        this.stepId = stepId;
    }

    public long getId() {
        return this.id;
    }

    @Override
    public long getJobExecutionId() {
        return rootJobExecution.getExecutionId();
    }

    @Override
    public String getName() {
        return rootJobExecution.getJobInstance().getJobName();
    }

    @Override
    public String getStepId() {
        return stepId;
    }

    @Override
    public P getUserPersistentData() {
        return userPersistentData;
    }

    public void setUserPersistentData(P userPersistentData) {
        this.userPersistentData = userPersistentData;
    }

    @Override
    public Metric[] getMetrics() {
        return stepMetrics.getMetrics();
    }

}
