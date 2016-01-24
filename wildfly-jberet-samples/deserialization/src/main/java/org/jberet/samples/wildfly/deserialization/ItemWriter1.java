/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.deserialization;

import java.io.Serializable;
import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Named;

@Named
public class ItemWriter1 extends AbstractItemWriter {
    @Override
    public void writeItems(final List<Object> items) throws Exception {
        System.out.printf("Writing %s%n", items);
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return new org.jberet.samples.wildfly.deserialization.CheckpointInfo1(1);
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        System.out.printf("Writer open with checkpoint %s%n", checkpoint);
    }
}
