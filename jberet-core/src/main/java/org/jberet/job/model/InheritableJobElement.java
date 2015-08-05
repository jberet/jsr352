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

/**
 * Abstract base class for those job elements that support JSL inheritance, such as job, step and flow.
 *
 * @see Job
 * @see Step
 * @see Flow
 */
public abstract class InheritableJobElement extends AbstractJobElement {
    private static final long serialVersionUID = 3900582466971487659L;

    private Listeners listeners;
    private boolean abstractAttribute;
    private String parent;
    private String jslName;

    protected InheritableJobElement(final String id) {
        super(id);
    }

    /**
     * Checks if this job element is abstract, which is specified by its {@code abstract} attribute. It defaults to
     * {@code false}.
     *
     * @return true if this job element is abstract; false otherwise
     */
    public boolean isAbstract() {
        return abstractAttribute;
    }

    /**
     * Sets the {@code abstract} attribute value for this job element.
     *
     * @param s a string value specifying true or false
     */
    void setAbstract(final String s) {
        if (s != null && s.toLowerCase().equals("true")) {
            this.abstractAttribute = true;
        }
    }

    /**
     * Gets the {@code parent} attribute value for this job element.
     *
     * @return the {@code parent} attribute value for this job element
     */
    String getParent() {
        return parent;
    }

    /**
     * Gets the {@code jsl-name} attribute for this job element.
     *
     * @return the {@code jsl-name} attribute for this job element
     */
    String getJslName() {
        return jslName;
    }

    /**
     * Sets the {@code parent} and {@code jsl-name} attribute value for this job element.
     *
     * @param parent the {@code parent} attribute value
     * @param jslName the {@code jsl-name} attribute value
     */
    void setParentAndJslName(final String parent, final String jslName) {
        this.parent = parent;
        this.jslName = jslName;
    }

    /**
     * Gets all listeners as {@link Listeners} for this job element.
     *
     * @return all listeners for this job element
     */
    public Listeners getListeners() {
        return listeners;
    }

    /**
     * Sets all listerners for this job element.
     *
     * @param listeners all listeners as {@code Listeners}
     */
    public void setListeners(final Listeners listeners) {
        this.listeners = listeners;
    }
}
