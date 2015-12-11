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
import java.util.HashSet;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public final class ChunkPartitionAnalyzer implements PartitionAnalyzer {
    @Inject
    private StepContext stepContext;

    @Inject @BatchProperty(name = "thread.count")
    private int threadCount;

    @Inject @BatchProperty(name = "skip.thread.check")
    private boolean skipThreadCheck;

    private final Set<Long> childThreadIds = new HashSet<Long>();
    private int numOfCompletedPartitions;

    @Override
    public void analyzeCollectorData(final Serializable data) throws Exception {
        childThreadIds.add((Long) data);
    }

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        //the check for number of threads used is not very accurate.  The underlying thread pool
        //may choose a cold thread even when a warm thread has already been returned to pool and available.
        //especially when thread.count is 1, there may be 2 or more threads being used, but at one point,
        //there should be only 1 active thread running partition.
        numOfCompletedPartitions++;
        if(numOfCompletedPartitions == 3  && !skipThreadCheck) { //partitions in job xml
            if (childThreadIds.size() <= threadCount) {  //threads in job xml
                stepContext.setExitStatus(String.format("PASS: Max allowable thread count %s, actual threads %s",
                        threadCount, childThreadIds.size()));
            } else {
                stepContext.setExitStatus(String.format("FAIL: Expecting max thread count %s, but got %s",
                        threadCount, childThreadIds.size()));
            }
        }
    }
}
