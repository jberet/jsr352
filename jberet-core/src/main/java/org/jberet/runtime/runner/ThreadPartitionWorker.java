/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.runner;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.spi.PartitionWorker;

public class ThreadPartitionWorker implements PartitionWorker {
    private BlockingQueue<Boolean> completedPartitionThreads;

    private BlockingQueue<Serializable> collectorDataQueue;

    public ThreadPartitionWorker(final BlockingQueue<Boolean> completedPartitionThreads,
                                 final BlockingQueue<Serializable> collectorDataQueue) {
        this.completedPartitionThreads = completedPartitionThreads;
        this.collectorDataQueue = collectorDataQueue;
    }

    @Override
    public void reportData(final Serializable data,
                           final AbstractStepExecution partitionExecution) throws Exception {
        collectorDataQueue.put(data);
    }

    @Override
    public void partitionDone(final AbstractStepExecution partitionExecution) throws Exception {
        if (completedPartitionThreads != null) {
            completedPartitionThreads.offer(Boolean.TRUE);
        }
        collectorDataQueue.put(partitionExecution);
    }
}
