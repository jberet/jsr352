/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Corresponds to {@code jsl:PartitionPlan} job element type in job XML.
 */
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

    /**
     * list of {@code org.jberet.job.model.Properties}, one properties for each partition.
     */
    private List<Properties> propertiesList = new ArrayList<Properties>();

    public PartitionPlan() {
    }

    /**
     * Gets the list of {@code org.jberet.job.model.Properties} for this partition plan.
     *
     * @return list of {@code org.jberet.job.model.Properties}
     */
    public List<Properties> getPropertiesList() {
        return propertiesList;
    }

    public void setPropertiesList(final List<Properties> propertiesList) {
        this.propertiesList = propertiesList;
    }

    /**
     * Adds a {@code org.jberet.job.model.Properties} to the list of {@code org.jberet.job.model.Properties} for this
     * partition plan.
     *
     * @param properties a {@code org.jberet.job.model.Properties}
     */
    public void addProperties(final Properties properties) {
        propertiesList.add(properties);
    }

    /**
     * Gets the number of partition as string.
     *
     * @return number of partitions as string
     */
    public String getPartitions() {
        return partitions;
    }

    /**
     * Gets the number of partitions as int.
     *
     * @return number of partitions as int
     */
    public int getPartitionsInt() {
        if (partitions == null) {
            return DEFAULT_PARTITIONS;
        }
        return Integer.parseInt(partitions);
    }

    /**
     * Sets the number of partitions from a string parameter.
     *
     * @param partitions number of partitions as string
     */
    public void setPartitions(final String partitions) {
        if (partitions != null) {
            this.partitions = partitions;
        }
    }

    /**
     * Gets the number of threads as string.
     *
     * @return number of threads as string
     */
    public String getThreads() {
        return threads;
    }

    /**
     * Gets the number of threads as int. If {@code threads} attribute is not present, this method returns the
     * number of partitions.
     *
     * @return number of threads as int
     */
    public int getThreadsInt() {
        if (threads == null) {
            return getPartitionsInt();
        }
        return Integer.parseInt(threads);
    }

    /**
     * Sets the number of threads from a string parameter.
     *
     * @param threads number of threads as string
     */
    public void setThreads(final String threads) {
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
