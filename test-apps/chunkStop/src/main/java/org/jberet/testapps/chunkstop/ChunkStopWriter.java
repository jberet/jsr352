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

import java.util.List;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.runtime.Metric;
import javax.inject.Named;

@Named("chunkStopWriter")
public final class ChunkStopWriter extends IntegerArrayReaderWriterBase implements ItemWriter {
    @Override
    public void writeItems(List<Object> items) throws Exception {
        if (items == null) {
            return;
        }

        int writerFailAtInt = Integer.parseInt(writerFailAt);
        if (getMetric(Metric.MetricType.WRITE_COUNT) + items.size() >= writerFailAtInt && writerFailAtInt >= 0) {
            throw new ArithmeticException("Failing at writer.fail.at point " + writerFailAt);
        }
        Thread.sleep(Long.parseLong(writerSleepTime));
        for (Object o : items) {
            data[cursor] = (Integer) o;
            cursor++;
        }
        System.out.printf("Wrote items: %s%n", String.valueOf(items));
    }
}
