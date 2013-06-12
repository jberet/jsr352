/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.runtime.metric;

import java.io.Serializable;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.StepContext;

public class MetricImpl implements Metric, Serializable {

    private static final long serialVersionUID = 1L;

    private MetricType type;
    private long value;

    public MetricImpl(MetricType type) {
        this.type = type;
    }

    public MetricImpl(MetricType type, long value1) {
        this.type = type;
        this.value = value1;
    }

    @Override
    public MetricType getType() {
        return type;
    }

    @Override
    public long getValue() {
        return value;
    }

    public void setValue(long value1) {
        this.value = value1;
    }

    public void increment(long i) {
        this.value += i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricImpl)) return false;

        MetricImpl metric = (MetricImpl) o;
        return type == metric.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MetricImpl");
        sb.append("{name='").append(type).append('\'');
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    public static long getMetric(StepExecution stepExecution, Metric.MetricType type) {
        for (Metric m : stepExecution.getMetrics()) {
            if (m.getType() == type) {
                return m.getValue();
            }
        }
        return 0;
    }

    public static long getMetric(StepContext stepContext, Metric.MetricType type) {
        for (Metric m : stepContext.getMetrics()) {
            if (m.getType() == type) {
                return m.getValue();
            }
        }
        return 0;
    }
}
