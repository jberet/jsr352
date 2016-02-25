/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.entity;

import java.io.Serializable;
import javax.batch.runtime.Metric;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a step execution metric, which includes metric type and metric value.
 *
 * @see javax.batch.runtime.Metric
 *
 * @since 1.3.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MetricEntity implements Serializable, Metric {
    private static final long serialVersionUID = 717668174913816112L;

    @XmlElement
    private MetricType type;

    @XmlElement
    private long value;

    public MetricEntity() {
    }

    public MetricEntity(final Metric metric) {
        this.type = metric.getType();
        this.value = metric.getValue();
    }

    public static MetricEntity[] copyOf(final Metric[] metrics) {
        if (metrics != null) {
            MetricEntity[] metricArray = new MetricEntity[metrics.length];
            for (int i = 0; i < metricArray.length; i++) {
                metricArray[i] = new MetricEntity(metrics[i]);
            }
            return metricArray;
        } else {
            return null;
        }
    }

    @Override
    public MetricType getType() {
        return type;
    }

    public void setType(final MetricType type) {
        this.type = type;
    }

    @Override
    public long getValue() {
        return value;
    }

    public void setValue(final long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MetricData{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }
}
