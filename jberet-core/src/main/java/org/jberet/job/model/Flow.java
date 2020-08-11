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

import java.util.ArrayList;
import java.util.List;

/**
 * Corresponds to job element type {@code jsl:Flow} in job XML.
 */
public final class Flow extends InheritableJobElement {
    private static final long serialVersionUID = 6569569970633169427L;

    String next;

    /**
     * list of job elements in this flow, which may contain all types of job-level job elements:
     * <ul>
     *     <li>{@link Step}
     *     <li>{@link Flow}
     *     <li>{@link Split}
     *     <li>{@link Decision}
     * </ul>
     */
    List<JobElement> jobElements = new ArrayList<JobElement>();

    public Flow(final String id) {
        super(id);
    }

    /**
     * Gets the value of the {@code next} attribute, which specifies the next job element after this flow completes.
     *
     * @return {@code next} attribute value
     */
    public String getAttributeNext() {
        return next;
    }

    public void setAttributeNext(final String next) {
        this.next = next;
    }

    /**
     * Gets the list of job elements contained inside this flow.
     *
     * @return list of job elements
     */
    public List<JobElement> getJobElements() {
        return jobElements;
    }

    public void setJobElements(final List<JobElement> jobElements) {
        this.jobElements = jobElements;
    }

    /**
     * Disables getting listeners, since flow does not have listeners.
     *
     * @return N/A
     * @throws IllegalStateException always
     */
    @Override
    public Listeners getListeners() {
        return new Listeners();
    }

    /**
     * Disables setting listeners, since flow does not have listeners.
     * @param listeners N/A
     * @throws IllegalStateException always
     */
    @Override
    public void setListeners(final Listeners listeners) {

    }

    /**
     * Disables getting properties, since flow does not have properties.
     *
     * @return N/A
     * @throws IllegalStateException always
     */
    @Override
    public Properties getProperties() {
        return new Properties();
    }

    /**
     * Disables setting properties, since flow does not have properties.
     * @param properties N/A
     * @throws IllegalStateException always
     */
    @Override
    public void setProperties(final Properties properties) {

    }
}
