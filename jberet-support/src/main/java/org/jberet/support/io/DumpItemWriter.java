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

@Named
public class DumpItemWriter implements ItemWriter {
    @Override
    public void open(final Serializable checkpoint) throws Exception {

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        System.out.printf("%nNumber of items %s, element type %s%n", items.size(), items.get(0).getClass());
        for (final Object e : items) {
            System.out.printf("%s%n%n", e);
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
