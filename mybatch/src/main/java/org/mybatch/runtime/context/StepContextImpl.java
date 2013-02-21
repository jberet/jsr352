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
import javax.batch.api.StepListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Step;
import org.mybatch.runtime.metric.MetricName;
import org.mybatch.runtime.metric.StepMetrics;
import org.mybatch.util.BatchUtil;
import org.mybatch.util.PropertyResolver;

public class StepContextImpl<T, P extends Externalizable> extends BatchContextImpl<T> implements StepContext<T, P> {
    private Step step;
    private long stepExecutionId;
    private P persistentUserData;
    private Exception exception;
    private StepMetrics stepMetrics = new StepMetrics();

    private StepListener[] stepListeners;

    private StepContextImpl(Step step, long stepExecutionId1) {
        super(step.getId());
        this.step = step;
        this.stepExecutionId = stepExecutionId1;
    }

    public StepContextImpl(Step step, long stepExecutionId1, JobContextImpl<T> jobContext1) {
        this(step, stepExecutionId1);
        this.jobContext = jobContext1;
        this.classLoader = jobContext.getClassLoader();
        resolveProperties();
        initStepListeners();
    }

    public StepListener[] getStepListeners() {
        return stepListeners;
    }

    @Override
    public long getStepExecutionId() {
        return stepExecutionId;
    }

    @Override
    public Properties getProperties() {
        return BatchUtil.toJavaUtilProperties(step.getProperties());
    }

    @Override
    public P getPersistentUserData() {
        return persistentUserData;
    }

    @Override
    public void setPersistentUserData(P data) {
        this.persistentUserData = data;
    }

    @Override
    public JobOperator.BatchStatus getBatchStatus() {
        return null;
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
        return stepMetrics.getMetrics();
    }

    public void setMetric(MetricName metricName, long value) {
        stepMetrics.set(metricName, value);
    }

    private void initStepListeners() {
        Listeners listeners = step.getListeners();
        if (listeners != null) {
            List<Listener> listenerList = listeners.getListener();
            int count = listenerList.size();
            this.stepListeners = new StepListener[count];
            for (int i = 0; i < count; i++) {
                Listener listener = listenerList.get(i);
                this.stepListeners[i] = jobContext.createArtifact(listener.getRef(), listener.getProperties(), this);
            }
        }
    }

    private void resolveProperties() {
        PropertyResolver resolver = new PropertyResolver();
        resolver.setJobParameters(jobContext.getJobParameters());

        org.mybatch.job.Properties props = jobContext.getJob().getProperties();
        if (props != null) {
            resolver.pushJobProperties(props);
        }

        props = step.getProperties();
        if (props != null) {
            resolver.pushJobProperties(props);
        }

        resolver.resolve(step);
    }
}
