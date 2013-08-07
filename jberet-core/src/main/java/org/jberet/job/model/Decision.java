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

public final class Decision extends AbstractJobElement {
    private static final long serialVersionUID = -7022222093403964947L;

    private final String ref;
    private Properties properties;

    Decision(String id, String ref) {
        super(id);
        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public Properties getProperties() {
        return properties;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }
}
