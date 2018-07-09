/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunkstop;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public final class ChunkStopProcessor implements ItemProcessor {
    @Inject
    @BatchProperty
    private boolean throwException;

    @Override
    public Object processItem(final Object item) throws Exception {
        if (throwException) {
            throw new ChunkStopProcessorException("Configured to throw RuntimeException from " + this + ", item: " + item);
        }
        return item;
    }

    public static final class ChunkStopProcessorException extends Exception {
        public ChunkStopProcessorException(final String message) {
            super(message);
        }
    }
}
