/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import java.io.Serializable;
import java.util.Arrays;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("integerArrayReader")
public class IntegerArrayReader extends IntegerArrayReaderWriterProcessorBase implements ItemReader {
    @Inject
    @BatchProperty(name = "data.count")
    protected Integer dataCount;

    protected Integer[] data;

    protected int cursor;

    @Override
    public Object readItem() throws Exception {
        if (cursor > partitionEnd || cursor < partitionStart) {
            return null;
        }

        final Integer result = data[cursor];
        cursor++;

        if (failOnValues != null && Arrays.binarySearch(failOnValues, result) >= 0 &&
                (repeatFailure || !failedValues.contains(result))) {
            failedValues.add(result);
            throw new ArithmeticException("Failing on value " + Arrays.toString(failOnValues));
        }

        return result;
    }

    /**
     * Creates the data array without filling the data.
     */
    protected void initData() {
        if (dataCount == null) {
            throw new IllegalStateException("data.count property is not injected.");
        }
        data = new Integer[dataCount];

        if (partitionEnd == null) {
            partitionEnd = dataCount - 1;
        }

        for (int i = 0; i < dataCount; i++) {
            data[i] = i;
        }

        System.out.printf("init data: %s%n", Arrays.toString(data));

        //position the cursor according to partition start
        cursor = partitionStart;
        System.out.printf("Partition start = %s, end = %s in %s%n", partitionStart, partitionEnd, this);
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (data == null) {
            initData();
        }
        cursor = checkpoint == null ? partitionStart : (Integer) checkpoint;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return cursor;
    }
}
