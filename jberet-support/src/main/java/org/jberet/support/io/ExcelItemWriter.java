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
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} for Excel files.
 *
 * @see ExcelUserModelItemReader
 * @since 1.0.3
 */
@Named
@Dependent
public class ExcelItemWriter extends ExcelItemReaderWriterBase implements ItemWriter {

    @Override
    public void open(final Serializable checkpoint) throws Exception {

    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {

    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
