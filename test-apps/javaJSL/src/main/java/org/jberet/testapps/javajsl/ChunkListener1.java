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
import jakarta.batch.api.chunk.listener.AbstractChunkListener;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ChunkListener1 extends AbstractChunkListener {
    @Inject
    @BatchProperty
    private String stepExitStatus;

    @Inject
    private StepContext stepContext;

    @Override
    public void afterChunk() throws Exception {
        stepContext.setExitStatus(stepExitStatus);
    }
}
