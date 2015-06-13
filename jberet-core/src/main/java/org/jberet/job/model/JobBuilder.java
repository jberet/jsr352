/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;

public final class JobBuilder extends AbstractPropertiesBuilder<JobBuilder> {
    private final String id;
    private String restartable;
    private Listeners listeners;
    private final List<JobElement> jobElements = new ArrayList<JobElement>();

    public JobBuilder(final String id) {
        this.id = id;
    }

    public JobBuilder restartable(final boolean b) {
        this.restartable = String.valueOf(b);
        return this;
    }

    public JobBuilder listener(final String ref, final String[]... propKeysValues) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(createRefArtifactWithProperties(ref, null, propKeysValues));
        return this;
    }

    public JobBuilder listener(final String ref, final java.util.Properties props) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(createRefArtifactWithProperties(ref, props));
        return this;
    }

    public JobBuilder step(final Step step) {
        jobElements.add(step);
        return this;
    }

    public JobBuilder decision(final Decision decision) {
        jobElements.add(decision);
        return this;
    }

    public JobBuilder flow(final Flow flow) {
        jobElements.add(flow);
        return this;
    }

    public JobBuilder split(final Split split) {
        jobElements.add(split);
        return this;
    }

    public Job build() {
        final Job job = new Job(id);

        if (restartable != null) {
            job.setRestartable(restartable);
        }
        if (nameValues.size() > 0) {
            job.setProperties(nameValuesToProperties(nameValues));
        }

        job.setListeners(listeners);
        for (final JobElement jobElement : jobElements) {
            job.addJobElement(jobElement);
        }
        return job;
    }

    static RefArtifact createRefArtifactWithProperties(final String ref,
                                                       final java.util.Properties props,
                                                       final String[]... propKeysValues) {
        final RefArtifact refArtifact = new RefArtifact(ref);
        final Properties properties = new Properties();
        if (props != null) {
            for (final String k : props.stringPropertyNames()) {
                properties.add(k, props.getProperty(k));
            }
        } else if (propKeysValues.length > 0) {
            for (final String[] pair : propKeysValues) {
                properties.add(pair[0], pair.length > 1 ? pair[1] : null);
            }
        }
        refArtifact.setProperties(properties);
        return refArtifact;
    }
}
