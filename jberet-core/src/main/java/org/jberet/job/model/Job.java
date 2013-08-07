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

public final class Job implements Serializable {
    private static final long serialVersionUID = -3566969844084046522L;

    private final String id;
    private boolean restartable;
    private Properties properties;
    private final List<RefArtifact> listeners = new ArrayList<RefArtifact>();
    private final List<JobElement> jobElements = new ArrayList<JobElement>();

    Job(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean getRestartable() {
        return restartable;
    }

    void setRestartable(String restartable) {
        if (restartable != null) {
            this.restartable = Boolean.parseBoolean(restartable);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<RefArtifact> getListeners() {
        return listeners;
    }

    void addListener(RefArtifact listener) {
        listeners.add(listener);
    }

    void addListeners(List<RefArtifact> ls) {
        listeners.addAll(ls);
    }

    public List<JobElement> getJobElements() {
        return jobElements;
    }

    void addJobElement(JobElement jobElement) {
        jobElements.add(jobElement);
    }
}
