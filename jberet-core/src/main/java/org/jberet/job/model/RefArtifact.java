/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.io.Serializable;

import org.jberet._private.BatchMessages;

/**
 * Represents a batch artifact with a ref and properties.  It may be extended to form more complex artifact types.
 * Batch artifact types represented by this class include:
 * <ul>
 *     <li>{@code jsl:PartitionReducer}
 *     <li>{@code jsl:Analyzer}
 *     <li>{@code jsl:Collector}
 *     <li>{@code jsl:PartitionMapper}
 *     <li>{@code jsl:ItemWriter}
 *     <li>{@code jsl:ItemProcessor}
 *     <li>{@code jsl:ItemReader}
 *     <li>{@code jsl:Batchlet}
 *     <li>{@code jsl:CheckpointAlgorithm}
 *     <li>{@code jsl:Listener}
 * </ul>
 */
public class RefArtifact implements Serializable, Cloneable, PropertiesHolder {
    private static final long serialVersionUID = -3101663828339367848L;

    private String ref;
    private Properties properties;
    private Script script;

    public RefArtifact(final String ref) {
        this.ref = ref == null ? "" : ref;
    }

    /**
     * Gets the ref value for this batch artifact.
     *
     * @return the ref value for this batch artifact
     */
    public String getRef() {
        return ref;
    }

    /**
     * Sets the ref value for this batch artifact.
     *
     * @param ref the ref value for this batch artifact
     */
    public void setRef(final String ref) {
        this.ref = ref;
    }

    /**
     * Gets the {@linkplain Properties org.jberet.job.model.Properties} belonging to this batch artifact.
     *
     * @return org.jberet.job.model.Properties for this batch artifact
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the {@linkplain Properties org.jberet.job.model.Properties} belonging to this batch artifact.
     *
     * @param properties org.jberet.job.model.Properties for this batch artifact
     */
    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    /**
     * Gets the script that is either included in or referenced by this batch artifact in job XML.
     *
     * @return the script that implements this batch artifact
     */
    public Script getScript() {
        return script;
    }

    /**
     * Sets the script that is either included in or referenced by this batch artifact in job XML.
     * @param script the script that implements this batch artifact
     */
    public void setScript(final Script script) {
        if (this.ref.isEmpty()) {
            this.script = script;
        } else {
            throw BatchMessages.MESSAGES.cannotHaveBothScriptAndRef(this.ref);
        }
    }

    @Override
    protected RefArtifact clone() {
        final RefArtifact c = new RefArtifact(this.ref);
        if (properties != null) {
            c.setProperties(properties.clone());
        }
        if (this.script != null) {
            c.script = this.script.clone();
        }
        return c;
    }
}
