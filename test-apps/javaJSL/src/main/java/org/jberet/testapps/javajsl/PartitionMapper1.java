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

import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionMapper1 implements PartitionMapper {
    @Inject
    @BatchProperty
    private int partitionCount;

    @Override
    public PartitionPlan mapPartitions() throws Exception {
        final Properties[] partitionProps = new Properties[partitionCount];

        final Properties p1 = new Properties();
        p1.setProperty("partition.start", "0");
        p1.setProperty("partition.end", "9");
        partitionProps[0] = p1;

        final Properties p2 = new Properties();
        p2.setProperty("partition.start", "10");
        p2.setProperty("partition.end", "19");
        partitionProps[1] = p2;

        final Properties p3 = new Properties();
        p3.setProperty("partition.start", "20");
        p3.setProperty("partition.end", "29");
        partitionProps[2] = p3;

        final PartitionPlan plan = new PartitionPlanImpl();
        plan.setPartitions(partitionCount);
        plan.setThreads(partitionCount);
        plan.setPartitionProperties(partitionProps);

        return plan;
    }
}
