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
import java.util.HashMap;
import java.util.Map;

/**
 * Corresponds to batch.xml root element type {@code batch-artifacts}. An example batch.xml file:
 * <pre>
 * &lt;batch-artifacts xmlns="http://xmlns.jcp.org/xml/ns/javaee"&gt;
 *      &lt;ref id="batchlet1" class="org.jberet.testapps.common.Batchlet1"/&gt;
 *      &lt;ref id="L4" class="org.jberet.testapps.loadBatchXml.StepListener4"/&gt;
 * &lt;/batch-artifacts&gt;
 * </pre>
 */
public final class BatchArtifacts implements Serializable {
    private static final long serialVersionUID = -1743162829437308414L;

    private final Map<String, String> refs = new HashMap<String, String>();

    /**
     * Adds a batch artifact ref by its id and fully-qualified class name.
     *
     * @param id id (name) of the batch artifact ref
     * @param clazz fully-qualified class name of the batch artifact
     */
    void addRef(final String id, final String clazz) {
        refs.put(id, clazz);
    }

    /**
     * Gets the fully-qualified class name for a batch artifact by its ref name.
     *
     * @param ref name of the batch artifact ref
     * @return fully-qualified class name of the batch artifact
     */
    public String getClassNameForRef(final String ref) {
        return refs.get(ref);
    }
}
