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

public final class PartitionPlan implements Serializable {
    private static final long serialVersionUID = -7038781842409368148L;

    /**
     * Specifies the number of partitions for this partitioned step. This is a an optional attribute. The default is 1.
     */
    private int partitions = 1;

    /**
     * Specifies the maximum number of threads on which to execute the partitions of this step.
     * Note the batch runtime cannot guarantee the requested number of threads are available;
     * it will use as many as it can up to the requested maximum. This is an optional attribute.
     * The default is the number of partitions.
     */
    private int threads = partitions;

    private Properties properties;

    PartitionPlan() {
    }

    public Properties getProperties() {
        return properties;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }

    public int getPartitions() {
        return partitions;
    }

    void setPartitions(String partitions) {
        if (partitions != null) {
            this.partitions = Integer.parseInt(partitions);
        }
    }

    public int getThreads() {
        return threads;
    }

    void setThreads(String threads) {
        if (threads != null) {
            this.threads = Integer.parseInt(threads);
        }
    }
}
