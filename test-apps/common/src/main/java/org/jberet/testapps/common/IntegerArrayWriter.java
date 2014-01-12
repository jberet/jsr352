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

package org.jberet.testapps.common;

import java.util.List;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.runtime.Metric;
import javax.inject.Named;

import org.jberet.runtime.context.StepContextImpl;
import org.jberet.runtime.metric.MetricImpl;

@Named("integerArrayWriter")
public final class IntegerArrayWriter extends IntegerArrayReaderWriterBase implements ItemWriter {
    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (items == null) {
            return;
        }

        final long writeCount = MetricImpl.getMetric(((StepContextImpl) stepContext).getStepExecution(), Metric.MetricType.WRITE_COUNT);
        if (writeCount + items.size() >= writerFailAt
                && writerFailAt >= 0) {
            System.out.printf("About to throw ArithmeticException for writerFailAt %s, WRITE_COUNT %s", writerFailAt, writeCount);
            throw new ArithmeticException("Failing at writer.fail.at point " + writerFailAt);
        }
        if (writerSleepTime > 0) {
            Thread.sleep(writerSleepTime);
        }
        for (final Object o : items) {
            data[cursor] = (Integer) o;
            cursor++;
        }
        System.out.printf("Wrote items: %s%n", String.valueOf(items));
    }

    @Override
    protected void initData() {
        super.initData();
        cursor = partitionStart;
        System.out.printf("Partition start = %s, end = %s in %s%n", partitionStart, partitionEnd, this);
    }
}
