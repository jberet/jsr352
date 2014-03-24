/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

public abstract class InheritableJobElement extends AbstractJobElement {
    private static final long serialVersionUID = 3900582466971487659L;

    private Listeners listeners;
    private boolean abstractAttribute;
    private String parent;
    private String jslName;

    protected InheritableJobElement(final String id) {
        super(id);
    }

    boolean isAbstract() {
        return abstractAttribute;
    }

    void setAbstract(final String s) {
        if (s != null && s.toLowerCase().equals("true")) {
            this.abstractAttribute = true;
        }
    }

    String getParent() {
        return parent;
    }

    String getJslName() {
        return jslName;
    }

    void setParentAndJslName(final String parent, final String jslName) {
        this.parent = parent;
        this.jslName = jslName;
    }

    public Listeners getListeners() {
        return listeners;
    }

    public void setListeners(final Listeners listeners) {
        this.listeners = listeners;
    }
}
