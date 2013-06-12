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

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public abstract class IntegerArrayReaderWriterBase {
    @Inject
    protected StepContext stepContext;

    @Inject @BatchProperty(name = "data.count")
    protected Integer dataCount;

    @Inject @BatchProperty(name = "partition.start")
    protected int partitionStart;

    @Inject @BatchProperty(name = "partition.end")
    protected Integer partitionEnd;

    @Inject @BatchProperty(name = "reader.fail.at")
    protected Integer readerFailAt;

    @Inject @BatchProperty(name = "writer.fail.at")
    protected Integer writerFailAt;

    @Inject @BatchProperty(name = "writer.sleep.time")
    protected long writerSleepTime;

    protected Integer[] data;
    protected int cursor;

    protected IntegerArrayReaderWriterBase() {
        System.out.printf("Instantiating %s%n", this);
    }

    /**
     * Creates the data array without filling the data.
     */
    protected void initData() {
        if (dataCount == null) {
            throw new IllegalStateException("data.count property is not injected.");
        }
        data = new Integer[dataCount];
        if (readerFailAt == null) {
            readerFailAt = -1;
        }
        if (writerFailAt == null) {
            writerFailAt = -1;
        }
        if (partitionEnd == null) {
            partitionEnd = dataCount - 1;
        }
    }

    public void open(Serializable checkpoint) throws Exception {
        if (data == null) {
            initData();
        }
        cursor = checkpoint == null ? partitionStart : (Integer) checkpoint;
    }

    public Serializable checkpointInfo() throws Exception {
        return cursor;
    }

    public void close() throws Exception {
    }
}
