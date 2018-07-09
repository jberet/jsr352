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

import javax.batch.api.chunk.AbstractCheckpointAlgorithm;
import javax.inject.Named;

@Named
public final class CheckpointAlgorithm1 extends AbstractCheckpointAlgorithm {

    @Override
    public int checkpointTimeout() throws Exception {
        return 0;
    }

    @Override
    public void beginCheckpoint() throws Exception {
        super.beginCheckpoint();
    }

    @Override
    public void endCheckpoint() throws Exception {
        super.endCheckpoint();
    }

    @Override
    public boolean isReadyToCheckpoint() throws Exception {
        return true;
    }
}
