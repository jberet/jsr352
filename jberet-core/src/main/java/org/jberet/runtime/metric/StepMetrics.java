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

import java.util.HashMap;
import java.util.Map;
import javax.batch.runtime.Metric;

/**
 * Maintains execution metrics for a single step.
 */
final public class StepMetrics {

    private final Map<Metric.MetricType, MetricImpl> metricsMapping = new HashMap<Metric.MetricType, MetricImpl>();

    public StepMetrics() {
        for (Metric.MetricType m : Metric.MetricType.values()) {
            metricsMapping.put(m, new MetricImpl(m));
        }
    }

    public Metric[] getMetrics() {
        return metricsMapping.values().toArray(new Metric[metricsMapping.size()]);
    }

    public void set(Metric.MetricType name, long value) {
        MetricImpl targetMetric = metricsMapping.get(name);
        targetMetric.setValue(value);
    }

    public void increment(Metric.MetricType name, long value) {
        MetricImpl targetMetric = metricsMapping.get(name);
        targetMetric.increment(value);
    }

    @Override
    public String toString() {
        return "StepMetrics: " + metricsMapping;
    }
}
