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

public enum MetricName {
    READ_COUNT("readCount"),
    WRITE_COUNT("writeCount"),
    COMMIT_COUNT("commitCount"),
    ROLLBACK_COUNT("rollbackCount"),
    READ_SKIP_COUNT("readSkipCount"),
    PROCESS_SKIP_COUNT("processSkipCount"),
    FILTER_COUNT("filterCount"),
    WRITE_SKIP_COUNT("writeSkipCount");

    private String metricName;

    private MetricName(String s) {
        this.metricName = s;
    }

    @Override
    public String toString() {
        return metricName;
    }
}
