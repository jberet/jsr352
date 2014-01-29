/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.io.Serializable;
import java.util.List;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes data to CSV file or resource.
 */
@Named
public class CsvItemWriter extends CsvItemReaderWriterBase implements ItemWriter {
    @Override
    public void open(final Serializable checkpoint) throws Exception {

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        SupportLogger.LOGGER.aboutToWriteItems(items.size(), items.get(0).getClass());

    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
