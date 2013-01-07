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
 
package org.mybatch.runtime.metric;

import java.util.HashMap;
import java.util.Map;
import javax.batch.runtime.Metric;

import static org.mybatch.runtime.metric.MetricName.COMMIT_COUNT;
import static org.mybatch.runtime.metric.MetricName.FILTER_COUNT;
import static org.mybatch.runtime.metric.MetricName.PROCESS_SKIP_COUNT;
import static org.mybatch.runtime.metric.MetricName.READ_COUNT;
import static org.mybatch.runtime.metric.MetricName.READ_SKIP_COUNT;
import static org.mybatch.runtime.metric.MetricName.ROLLBACK_COUNT;
import static org.mybatch.runtime.metric.MetricName.WRITE_COUNT;
import static org.mybatch.runtime.metric.MetricName.WRITE_SKIP_COUNT;

/**
 * Maintains execution metrics for a single step.
 */
final public class StepMetrics {

    private final Map<MetricName, MetricImpl> metricsMapping = new HashMap<MetricName, MetricImpl>();

    public StepMetrics() {
        metricsMapping.put(READ_COUNT, new MetricImpl(READ_COUNT, 0));
        metricsMapping.put(WRITE_COUNT, new MetricImpl(WRITE_COUNT, 0));
        metricsMapping.put(COMMIT_COUNT, new MetricImpl(COMMIT_COUNT, 0));
        metricsMapping.put(ROLLBACK_COUNT, new MetricImpl(ROLLBACK_COUNT, 0));
        metricsMapping.put(READ_SKIP_COUNT, new MetricImpl(READ_SKIP_COUNT, 0));
        metricsMapping.put(PROCESS_SKIP_COUNT, new MetricImpl(PROCESS_SKIP_COUNT, 0));
        metricsMapping.put(FILTER_COUNT, new MetricImpl(FILTER_COUNT, 0));
        metricsMapping.put(WRITE_SKIP_COUNT, new MetricImpl(WRITE_SKIP_COUNT, 0));
    }

    public Metric[] getMetrics() {
        return metricsMapping.values().toArray(new Metric[metricsMapping.size()]);
    }

    public void updateMetric(MetricName name, long value) {
        MetricImpl targetMetric = metricsMapping.get(name);
        targetMetric.setValue(value);
    }
}
