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

import java.io.Serializable;
import javax.batch.runtime.Metric;

public class MetricImpl implements Metric, Serializable {

    private static final long serialVersionUID = 1L;

    private MetricName name;
    private long value;

    public MetricImpl(MetricName name1, long value1) {
        this.name = name1;
        this.value = value1;
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public long getValue() {
        return 0;
    }

    public void setValue(long value1) {
        this.value = value1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricImpl)) return false;

        MetricImpl metric = (MetricImpl) o;

        return name.equals(metric.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MetricImpl");
        sb.append("{name='").append(name).append('\'');
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
