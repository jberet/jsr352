/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.testapps.chunk;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.batch.runtime.context.StepContext;

import com.google.common.base.Objects;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ReaderWriterResult implements Serializable {

    private static final long serialVersionUID = 4791578864185317118L;

    private volatile boolean readerClosed;
    private AtomicInteger readCount;
    private volatile boolean writerClosed;
    private AtomicInteger writeCount;

    public ReaderWriterResult() {
        readerClosed = false;
        readCount = new AtomicInteger(0);
        writerClosed = false;
        writeCount = new AtomicInteger(0);
    }

    public boolean isReaderClosed() {
        return readerClosed;
    }

    protected void setReaderClosed(final boolean readerClosed) {
        this.readerClosed = readerClosed;
    }

    public int getReadCount() {
        return readCount.get();
    }

    public int incrementReadCount() {
        return readCount.incrementAndGet();
    }


    public boolean isWriterClosed() {
        return writerClosed;
    }

    protected void setWriterClosed(final boolean writerClosed) {
        this.writerClosed = writerClosed;
    }

    public int getWriteCount() {
        return writeCount.get();
    }

    public int incrementWriteCount() {
        return writeCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ReaderWriterResult.class)
                .add("readerClosed", readerClosed)
                .add("readCount", readCount)
                .add("writerClosed", writerClosed)
                .add("writeCount", writeCount)
                .toString();
    }

    static ReaderWriterResult getOrCreateReaderWriterItem(final StepContext stepContext) {
        ReaderWriterResult result = (ReaderWriterResult) stepContext.getPersistentUserData();
        if (result == null) {
            result = new ReaderWriterResult();
            stepContext.setPersistentUserData(result);
        }
        return result;
    }

    static ReaderWriterResult getReaderWriterItem(final StepContext stepContext) {
        return (ReaderWriterResult) stepContext.getPersistentUserData();
    }
}
