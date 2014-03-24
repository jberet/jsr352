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
import java.util.LinkedHashMap;
import java.util.Map;

public final class Properties implements Serializable, Cloneable {
    private static final long serialVersionUID = 7115483407256741761L;

    private String partition;
    private final Map<String, String> nameValues = new LinkedHashMap<String, String>();
    private boolean merge = true;

    Properties() {
    }

    void add(final String name, final String value) {
        nameValues.put(name, value);
    }

    void addIfAbsent(final String name, final String value) {
        if (!nameValues.containsKey(name)) {
            nameValues.put(name, value);
        }
    }

    void remove(final String name) {
        nameValues.remove(name);
    }

    public String get(final String name) {
        return nameValues.get(name);
    }

    boolean isMerge() {
        return merge;
    }

    void setMerge(final String mergeVal) {
        if (mergeVal != null && !mergeVal.toLowerCase().equals("true")) {
            this.merge = false;
        }
    }

    public String getPartition() {
        return partition;
    }

    void setPartition(final String partition) {
        this.partition = partition;
    }

    public int size() {
        return nameValues.size();
    }

    Map<String, String> getPropertiesMapping() {
        return new LinkedHashMap<String, String>(nameValues);
    }

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
        //merge attribute is not copied over
        c.setPartition(this.partition);
        for (final Map.Entry<String, String> e : this.nameValues.entrySet()) {
            c.add(e.getKey(), e.getValue());
        }
        return c;
    }
}
