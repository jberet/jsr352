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
import java.util.HashMap;
import java.util.Map;

public final class BatchArtifacts implements Serializable {
    private static final long serialVersionUID = -1743162829437308414L;

    private final Map<String, String> refs = new HashMap<String, String>();

    void addRef(final String id, final String clazz) {
        refs.put(id, clazz);
    }

    public String getClassNameForRef(final String ref) {
        return refs.get(ref);
    }
}
