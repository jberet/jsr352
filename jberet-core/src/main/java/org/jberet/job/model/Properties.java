/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Corresponds to {@code jsl:Properties} of job XML element type.
 */
public final class Properties extends MergeableElement implements Serializable, Cloneable {
    private static final long serialVersionUID = 7115483407256741761L;

    /**
     * The partition index, only applicable when this {@code org.jberet.job.model.Properties} belongs to a
     * {@link PartitionPlan}, which contains a list of such properties.
     * In other cases, the partition index value is usually not set.
     */
    private String partition;

    private Map<String, String> nameValues = new LinkedHashMap<String, String>();

    public Properties() {
    }

    /**
     * Adds one property by its name and value. Any existing property of the same name will be overwritten.
     *
     * @param name property name
     * @param value property value
     */
    void add(final String name, final String value) {
        nameValues.put(name, value);
    }

    /**
     * Adds one property by its name and value if the name does not exist.
     *
     * @param name property name
     * @param value property value
     */
    void addIfAbsent(final String name, final String value) {
        if (!nameValues.containsKey(name)) {
            nameValues.put(name, value);
        }
    }

    /**
     * Removes the property by its name.
     *
     * @param name property name
     */
    void remove(final String name) {
        nameValues.remove(name);
    }

    /**
     * Gets the value of a property by its name.
     *
     * @param name property name
     * @return the property value, null if the name does not exist
     */
    public String get(final String name) {
        return nameValues.get(name);
    }

    /**
     * Gets the partition index of this {@code org.jberet.job.model.Properties}. The partition index is only applicable
     * when this {@code org.jberet.job.model.Properties} belongs to a {@link PartitionPlan}, which contains a list of
     * such properties. In other cases, the partition index value is usually not set.
     *
     * @return the partition index for this properties
     */
    public String getPartition() {
        return partition;
    }

    /**
     * Sets the partition index for this {@code org.jberet.job.model.Properties}, and tells which partition this
     * properties belongs to.
     *
     * @param partition the partition index for this properties
     */
    public void setPartition(final String partition) {
        this.partition = partition;
    }

    /**
     * Gets the size of this {@code org.jberet.job.model.Properties}, i.e., how many entries this properties contains.
     *
     * @return size of this {@code org.jberet.job.model.Properties}
     */
    public int size() {
        return nameValues.size();
    }

    public Map<String, String> getNameValues() {
        return nameValues;
    }

    public void setNameValues(final Map<String, String> nameValues) {
        this.nameValues = nameValues;
    }

    /**
     * Gets a copy of the underlying mapping for this {@code org.jberet.job.model.Properties}.
     *
     * @return a copy of the underlying mapping
     */
    public Map<String, String> getPropertiesMapping() {
        return new LinkedHashMap<String, String>(nameValues);
    }

    /**
     * Converts an instance of this type of properties to {@code java.util.Properties}.
     *
     * @param p an instance of {@code org.jberet.job.model.Properties}
     * @return an instance of {@code java.util.Properties}
     */
    public static java.util.Properties toJavaUtilProperties(final Properties p) {
        final java.util.Properties props = new java.util.Properties();
        if (p != null) {
            for (final Map.Entry<String, String> e : p.nameValues.entrySet()) {
                props.setProperty(e.getKey(), e.getValue());
            }
        }
        return props;
    }

    @Override
    protected Properties clone() {
        final Properties c = new Properties();
        //merge attribute from super class is not copied over
        c.setPartition(this.partition);
        for (final Map.Entry<String, String> e : this.nameValues.entrySet()) {
            c.add(e.getKey(), e.getValue());
        }
        return c;
    }
}
