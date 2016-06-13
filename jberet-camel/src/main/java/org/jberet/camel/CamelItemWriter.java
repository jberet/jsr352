/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.camel;

import java.io.Serializable;
import java.util.List;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Named;

/**
 * Implementation of {@code javax.batch.api.chunk.ItemWriter} that writes batch data
 * to Apache Camel endpoint.
 *
 * @since 1.3.0
 */
@Named
public class CamelItemWriter extends CamelArtifactBase implements ItemWriter {
    @Override
    public void open(final Serializable checkpoint) throws Exception {

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {

    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
