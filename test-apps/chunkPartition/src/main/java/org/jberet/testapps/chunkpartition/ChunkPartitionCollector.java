/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunkpartition;

import java.io.Serializable;
import jakarta.batch.api.partition.PartitionCollector;
import jakarta.inject.Named;

@Named
public final class ChunkPartitionCollector implements PartitionCollector {
    @Override
    public Serializable collectPartitionData() throws Exception {
        return Thread.currentThread().getId();
    }
}
