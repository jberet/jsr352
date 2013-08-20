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

/**
 * Represents a batch artifact with a ref and properties.  It may be extended to form more complex artifact types.
 */
public class RefArtifact implements Serializable {
    private static final long serialVersionUID = -3101663828339367848L;

    private String ref;
    private Properties properties;

    RefArtifact(final String ref) {
        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    void setRef(final String ref) {
        this.ref = ref;
    }

    public Properties getProperties() {
        return properties;
    }

    void setProperties(final Properties properties) {
        this.properties = properties;
    }
}
