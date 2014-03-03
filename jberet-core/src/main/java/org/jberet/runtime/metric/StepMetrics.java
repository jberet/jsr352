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
import java.util.HashMap;
import java.util.Map;
import javax.batch.runtime.Metric;

/**
 * Maintains execution metrics for a single step.
 */
final public class StepMetrics implements Serializable {

    private static final long serialVersionUID = -4854359401644105419L;
    private final Map<Metric.MetricType, MetricImpl> metricsMapping = new HashMap<Metric.MetricType, MetricImpl>();

    public StepMetrics() {
        for (final Metric.MetricType m : Metric.MetricType.values()) {
            metricsMapping.put(m, new MetricImpl(m));
        }
    }

    public Metric[] getMetrics() {
        return metricsMapping.values().toArray(new Metric[metricsMapping.size()]);
    }

    public void set(final Metric.MetricType name, final long value) {
        final MetricImpl targetMetric = metricsMapping.get(name);
        targetMetric.setValue(value);
    }

    public long get(final Metric.MetricType name) {
        return metricsMapping.get(name).getValue();
    }

    public void increment(final Metric.MetricType name, final long value) {
        final MetricImpl targetMetric = metricsMapping.get(name);
        targetMetric.increment(value);
    }

    public void addStepMetrics(final StepMetrics other) {
        for (final Map.Entry<Metric.MetricType, MetricImpl> e : other.metricsMapping.entrySet()) {
            final long number = e.getValue().getValue();
            if (number > 0) {
                increment(e.getKey(), number);
            }
        }
    }

    @Override
    public String toString() {
        return "StepMetrics: " + metricsMapping;
    }
}
