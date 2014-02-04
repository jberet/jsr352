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
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Objects;

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
        ReaderWriterResult.getOrCreateReaderWriterItem(stepContext).setWriterClosed(false);
    }

    @Override
    public void close() throws Exception {
        ReaderWriterResult.getOrCreateReaderWriterItem(stepContext).setWriterClosed(true);
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return ReaderWriterResult.getReaderWriterItem(stepContext);
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        final ReaderWriterResult item = ReaderWriterResult.getOrCreateReaderWriterItem(stepContext);
        for (Object o : items) {
            if (item.incrementWriteCount() == failWriteAt) {
                throw new BatchRuntimeException("Failed writer at point " + failWriteAt + ". Reader and writer should both be closed.");
            }
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("failWriterAtOpen", failWriterAtOpen)
                .add("failWriteAt", failWriteAt)
                .add("readerWriterItem", ReaderWriterResult.getOrCreateReaderWriterItem(stepContext))
                .toString();
    }
}
