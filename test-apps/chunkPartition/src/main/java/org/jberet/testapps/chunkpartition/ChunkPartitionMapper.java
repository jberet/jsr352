/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunkpartition;

import java.util.Properties;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionPlanImpl;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ChunkPartitionMapper implements PartitionMapper {
    private static final int partitionCount = 3;

    /**
     * comma-separated values for which the reader will fail
     */
    @Inject
    @BatchProperty(name = "reader.fail.on.values")
    private String readerFailValues;

    /**
     * to configure mapper override setting
     */
    @Inject
    @BatchProperty(name = "override")
    private boolean override;

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

        if (override) {
            plan.setPartitionsOverride(true);
        }

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
