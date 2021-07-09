/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunk;

import java.io.Serializable;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.common.base.MoreObjects;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.context.StepContext;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class ClosingItemWriter extends AbstractItemWriter implements ItemWriter {

    @Inject
    private StepContext stepContext;

    @Inject
    @BatchProperty
    private boolean failWriterAtOpen;

    @Inject
    @BatchProperty
    private int failWriteAt;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (failWriterAtOpen) {
            throw new BatchRuntimeException("Failed writer at open");
        }
        stepContext.setPersistentUserData(getOrCreateReaderWriterResult().setWriterClosed(false));
    }

    @Override
    public void close() throws Exception {
        stepContext.setPersistentUserData(getOrCreateReaderWriterResult().setWriterClosed(true));
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return stepContext.getPersistentUserData();
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        final ReaderWriterResult item = getOrCreateReaderWriterResult();
        for (Object o : items) {
            try {
                if (item.incrementWriteCount() == failWriteAt) {
                    throw new BatchRuntimeException("Failed writer at point " + failWriteAt + ". Reader and writer should both be closed.");
                }
            } finally {
                stepContext.setPersistentUserData(item);
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("failWriterAtOpen", failWriterAtOpen)
                .add("failWriteAt", failWriteAt)
                .add("readerWriterItem", stepContext)
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
