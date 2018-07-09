/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.metric;

import java.io.Serializable;
import javax.batch.runtime.Metric;

import org.jberet.runtime.AbstractStepExecution;

public class MetricImpl implements Metric, Serializable {

    private static final long serialVersionUID = 1L;

    private final MetricType type;
    private long value;

    public MetricImpl(final MetricType type) {
        this.type = type;
    }

    @Override
    public MetricType getType() {
        return type;
    }

    @Override
    public long getValue() {
        return value;
    }

    public void setValue(final long value1) {
        this.value = value1;
    }

    public void increment(final long i) {
        this.value += i;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricImpl)) return false;

        final MetricImpl metric = (MetricImpl) o;
        return type == metric.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(type).append('=').append(value);
        return sb.toString();
    }

    public static long getMetric(final AbstractStepExecution stepExecution, final Metric.MetricType type) {
        for (final Metric m : stepExecution.getMetrics()) {
            if (m.getType() == type) {
                return m.getValue();
            }
        }
        return 0;
    }
}
