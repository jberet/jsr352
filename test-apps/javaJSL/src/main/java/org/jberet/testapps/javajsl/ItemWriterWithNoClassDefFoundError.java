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

import java.util.List;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ItemWriterWithNoClassDefFoundError extends AbstractItemWriter {
    @Inject
    @BatchProperty
    private boolean throwError;

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (throwError) {
            throw new NoClassDefFoundError("NoClassDefFoundError from " + this);
        }
        System.out.printf("Writing items: %s%n", items);
    }
}
