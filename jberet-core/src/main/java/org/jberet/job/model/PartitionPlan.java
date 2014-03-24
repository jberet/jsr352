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

package org.jberet.job.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class PartitionPlan implements Serializable, Cloneable {
    private static final long serialVersionUID = -7038781842409368148L;
    private static final int DEFAULT_PARTITIONS = 1;

    /**
     * Specifies the number of partitions for this partitioned step. This is a an optional attribute. The default is 1.
     */
    private String partitions;

    /**
     * Specifies the maximum number of threads on which to execute the partitions of this step.
     * Note the batch runtime cannot guarantee the requested number of threads are available;
     * it will use as many as it can up to the requested maximum. This is an optional attribute.
     * The default is the number of partitions.
     */
    private String threads;

    private final List<Properties> propertiesList = new ArrayList<Properties>();

    PartitionPlan() {
    }

    public List<Properties> getPropertiesList() {
        return propertiesList;
    }

    void addProperties(final Properties properties) {
        propertiesList.add(properties);
    }

    public String getPartitions() {
        return partitions;
    }

    public int getPartitionsInt() {
        if (partitions == null) {
            return DEFAULT_PARTITIONS;
        }
        return Integer.parseInt(partitions);
    }

    void setPartitions(final String partitions) {
        if (partitions != null) {
            this.partitions = partitions;
        }
    }

    public String getThreads() {
        return threads;
    }

    public int getThreadsInt() {
        if (threads == null) {
            return getPartitionsInt();
        }
        return Integer.parseInt(threads);
    }

    void setThreads(final String threads) {
        if (threads != null) {
            this.threads = threads;
        }
    }

    @Override
    protected PartitionPlan clone() {
        final PartitionPlan c = new PartitionPlan();
        if (this.partitions != null) {
            c.partitions = this.partitions;
        }
        if (this.threads != null) {
            c.threads = this.threads;
        }
        for (final Properties p : this.propertiesList) {
            c.propertiesList.add(p.clone());
        }

        return c;
    }
}
