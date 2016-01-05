/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
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

import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ChunkPartitionMapper implements PartitionMapper {
    private static final int partitionCount = 3;

    /**
     * comma-separated values for which the reader will fail
     */
    @Inject
    @BatchProperty(name = "reader.fail.on.values")
    private String readerFailValues;

    @Override
    public PartitionPlan mapPartitions() throws Exception {
        final Properties[] partitionPropsArray = new Properties[partitionCount];
        partitionPropsArray[0] = createPropertiesForPartition("0", "9");
        partitionPropsArray[1] = createPropertiesForPartition("10", "19");
        partitionPropsArray[2] = createPropertiesForPartition("20", "29");

        final PartitionPlan plan = new PartitionPlanImpl();
        plan.setPartitions(partitionCount);
        plan.setThreads(partitionCount);
        plan.setPartitionProperties(partitionPropsArray);
        return plan;
    }

    /**
     * Creates a {@code java.util.Properties} for a single partition.
     *
     * @param start the start position of data for this partition
     * @param end the end position of data for this partition
     *
     * @return a new {@code java.util.Properties}
     */
    private Properties createPropertiesForPartition(final String start, final String end) {
        final Properties props = new Properties();
        props.setProperty("partition.start", start);
        props.setProperty("partition.end", end);
        if (readerFailValues != null) {
            props.setProperty("reader.fail.on.values", readerFailValues);
        }
        return props;
    }
}
