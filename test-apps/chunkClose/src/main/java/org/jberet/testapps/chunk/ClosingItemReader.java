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
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.ItemReader;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.MoreObjects;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class ClosingItemReader extends AbstractItemReader implements ItemReader {

    @Inject
    private StepContext stepContext;

    @Inject
    @BatchProperty
    private boolean failReaderAtOpen;

    @Inject
    @BatchProperty
    private int stopReadAt;

    @Inject
    @BatchProperty
    private int failReadAt;

    @Override
    public void close() throws Exception {
        stepContext.setPersistentUserData(getOrCreateReaderWriterResult().setReaderClosed(true));
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (failReaderAtOpen) {
            throw new BatchRuntimeException("Failed reader at open");
        }
        stepContext.setPersistentUserData(getOrCreateReaderWriterResult().setReaderClosed(false));
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return stepContext.getPersistentUserData();
    }

    @Override
    public Object readItem() throws Exception {
        final ReaderWriterResult item = getOrCreateReaderWriterResult();
        final int counter = item.incrementReadCount();
        stepContext.setPersistentUserData(item);
        if (counter == stopReadAt) {
            return null;
        }
        if (counter == failReadAt) {
            throw new BatchRuntimeException("Failed reader at point " + failReadAt + ". Reader and writer should both be closed.");
        }
        return counter;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("failReaderAtOpen", failReaderAtOpen)
                .add("stopReaderAt", stopReadAt)
                .add("failReadAt", failReadAt)
                .add("readerWriterItem", stepContext.getPersistentUserData())
                .toString();
    }

    private ReaderWriterResult getOrCreateReaderWriterResult() {
        ReaderWriterResult result = (ReaderWriterResult) stepContext.getPersistentUserData();
        if (result == null) {
            result = new ReaderWriterResult();
        }
        return result;
    }
}
