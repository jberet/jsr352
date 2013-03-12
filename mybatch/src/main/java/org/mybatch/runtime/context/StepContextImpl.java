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

package org.mybatch.runtime.context;

import java.io.Externalizable;
import java.util.List;
import java.util.Properties;
import javax.batch.api.listener.StepListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Step;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.util.BatchUtil;

public class StepContextImpl<T, P extends Externalizable> extends AbstractContext<T> implements StepContext<T, P> {
    private Step step;
    private StepExecutionImpl<P> stepExecution;
    private Exception exception;

    private StepListener[] stepListeners = new StepListener[0];

    public StepContextImpl(Step step, AbstractContext<T>[] outerContexts) {
        super(step.getId(), outerContexts);
        this.step = step;
        this.classLoader = getJobContext().getClassLoader();
        setUpPropertyResolver().resolve(this.step);
        createStepListeners();

        this.stepExecution = new StepExecutionImpl<P>(getJobContext().getJobExecution(), id);
        this.stepExecution.setBatchStatus(JobOperator.BatchStatus.STARTING);
    }

    public Step getStep() {
        return this.step;
    }

    public StepListener[] getStepListeners() {
        return stepListeners;
    }

    public StepExecutionImpl<P> getStepExecution() {
        return this.stepExecution;
    }

    @Override
    public String getStepName() {
        return step.getId();
    }

    @Override
    public JobOperator.BatchStatus getBatchStatus() {
        return stepExecution.getBatchStatus();
    }

    @Override
    public void setBatchStatus(JobOperator.BatchStatus status) {
        stepExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return stepExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(String exitStatus) {
        stepExecution.setExitStatus(exitStatus);
    }

    @Override
    public long getStepExecutionId() {
        return stepExecution.getExecutionId();
    }

    @Override
    public Properties getProperties() {
        return BatchUtil.toJavaUtilProperties(step.getProperties());
    }

    @Override
    public org.mybatch.job.Properties getProperties2() {
        return step.getProperties();
    }

    @Override
    public P getPersistentUserData() {
        return stepExecution.getUserPersistentData();  //TODO UserPersistence or PersistentUser?
    }

    @Override
    public void setPersistentUserData(P data) {
        stepExecution.setUserPersistentData(data);
    }

    @Override
    public Exception getException() {
        return exception;
    }

    public void setException(Exception e) {
        this.exception = e;
    }

    @Override
    public Metric[] getMetrics() {
        return stepExecution.getMetrics();
    }

    private void createStepListeners() {
        Listeners listeners = step.getListeners();
        if (listeners != null) {
            List<Listener> listenerList = listeners.getListener();
            int count = listenerList.size();
            this.stepListeners = new StepListener[count];
            for (int i = 0; i < count; i++) {
                Listener listener = listenerList.get(i);
                //ask the root JobContext to create artifact
                this.stepListeners[i] = getJobContext().createArtifact(listener.getRef(), listener.getProperties(), this);
            }
        }
    }
}
