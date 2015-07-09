/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.javajsl;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.listener.AbstractChunkListener;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ChunkListenerWithNoClassDefFoundError extends AbstractChunkListener {
    @Inject
    @BatchProperty
    private boolean throwError;

    @Override
    public void beforeChunk() throws Exception {
        if (throwError) {
            throw new NoClassDefFoundError("NoClassDefFoundError from " + this);
        }
        System.out.printf("In beforeChunk of %s%n", this);
    }
}
