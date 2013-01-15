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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Properties;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import org.mybatch.runtime.metric.MetricName;
import org.mybatch.runtime.metric.StepMetrics;

public class StepContextImpl<T, P> extends BatchContextImpl<T> implements StepContext<T, P> {
    private long stepExecutionId;
    private Properties properties;
    private P persistentUserData;
    private Exception exception;
    private StepMetrics stepMetrics = new StepMetrics();

    public StepContextImpl(String id, long stepExecutionId1) {
        super(id);
        this.stepExecutionId = stepExecutionId1;
    }

    public StepContextImpl(String id, long stepExecutionId1, JobContextImpl<T> jobContext1, Properties p) {
        this(id, stepExecutionId1);
        this.jobContext = jobContext1;
        this.properties = p;
    }

    @Override
    public long getStepExecutionId() {
        return stepExecutionId;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties p) {
        this.properties = p;
    }

    public void addProperty(String key, String value) {
        properties.setProperty(key, value);
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        //TODO
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        //TODO
    }
}
