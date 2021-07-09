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

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ItemProcessorWithNoClassDefFoundError implements ItemProcessor {
    @Inject
    @BatchProperty
    private boolean throwError;

    @Override
    public Object processItem(final Object item) throws Exception {
        if (throwError) {
            throw new NoClassDefFoundError("NoClassDefFoundError from " + this);
        }
        return item;
    }
}
