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
