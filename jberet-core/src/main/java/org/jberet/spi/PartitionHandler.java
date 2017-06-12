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

package org.jberet.spi;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

import org.jberet.runtime.context.StepContextImpl;

public interface PartitionHandler {
    void setResourceTracker(BlockingQueue<Boolean> completedPartitionThreads);

    void setCollectorDataQueue(BlockingQueue<Serializable> collectorDataQueue);

    void submitPartitionTask(StepContextImpl partitionStepContext) throws Exception;

    default void close(StepContextImpl stepContext) {}
}
