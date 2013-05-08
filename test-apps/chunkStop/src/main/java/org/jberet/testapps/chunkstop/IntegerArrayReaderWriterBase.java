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
 
package org.jberet.testapps.chunkstop;

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public abstract class IntegerArrayReaderWriterBase {
    @Inject
    protected StepContext stepContext;

    @Inject
    @BatchProperty(name = "data.count")
    protected String dataCountProp;
    int dataCount;

    @Inject
    @BatchProperty(name = "reader.fail.at")
    protected String readerFailAt = "-1";

    @Inject
    @BatchProperty(name = "writer.fail.at")
    protected String writerFailAt = "-1";

    @Inject
    @BatchProperty(name = "writer.sleep.time")
    protected String writerSleepTime;

    protected Integer[] data;
    protected int cursor;

    protected IntegerArrayReaderWriterBase() {
        System.out.printf("Instantiating %s%n", this);
    }

    /**
     * Creates the data array without filling the data.
     */
    protected void initData() {
        if (dataCountProp == null) {
            throw new IllegalStateException("data.count property is not injected.");
        }
        dataCount = Integer.parseInt(dataCountProp);
        data = new Integer[dataCount];
        if (readerFailAt == null) {
            readerFailAt = "-1";
        }
        if (writerFailAt == null) {
            writerFailAt = "-1";
        }
        if (writerSleepTime == null) {
            writerSleepTime = "0";
        }
    }

    protected long getMetric(Metric.MetricType type) {
        long result = 0;
        for (Metric m : stepContext.getMetrics()) {
            if (m.getType() == type) {
                result = m.getValue();
                break;
            }
        }
        return result;
    }

    public void open(Serializable checkpoint) throws Exception {
        if (data == null) {
            initData();
        }
        cursor = checkpoint == null ? 0 : (Integer) checkpoint;
    }

    public Serializable checkpointInfo() throws Exception {
        return cursor;
    }

    public void close() throws Exception {
    }
}
