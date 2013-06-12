/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.chunkpartition;

import java.io.Serializable;
import javax.batch.api.partition.PartitionCollector;
import javax.inject.Named;

@Named
public final class ChunkPartitionCollector implements PartitionCollector {
    @Override
    public Serializable collectPartitionData() throws Exception {
        return Thread.currentThread().getId();
    }
}
