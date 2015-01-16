/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
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

import java.io.Serializable;
import java.util.Arrays;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

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
            throw new ArithmeticException("Failing on value " + failOnValues);
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
