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

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.listener.AbstractChunkListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

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
