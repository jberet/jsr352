/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
 
package org.jberet.testapps.javajsl;

import java.util.Arrays;
import java.util.LinkedList;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ItemReaderWithNoClassDefFoundError extends AbstractItemReader {
    @Inject
    @BatchProperty
    private boolean throwError;

    private final LinkedList<Integer> data = new LinkedList<Integer>(Arrays.asList(1, 2, 3, null));

    @Override
    public Object readItem() throws Exception {
        if (throwError) {
            throw new NoClassDefFoundError("NoClassDefFoundError from " + this);
        }
        return data.remove(0);
    }
}
