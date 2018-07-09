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
import java.util.ArrayList;
import java.util.List;

/**
 * Corresponds to {@code jsl:Job} type, the root element of job XML.
 */
public final class Job extends InheritableJobElement implements Serializable, PropertiesHolder {
    private static final long serialVersionUID = -3566969844084046522L;

    /**
     * Internal value to indicate that a job execution cannot be restarted.
     *
     * @since 1.1.0
     */
    public static final String UNRESTARTABLE = "jberet.unrestartable";

    /**
     * Job parameter key to specify job xml name when it differs from job id.
     *
     * @see #getJobXmlName()
     * @see #setJobXmlName(String)
     * @since 1.1.0
     */
    public static final String JOB_XML_NAME = "jberet.jobXmlName";

    private String restartable;

    private final List<JobElement> jobElements = new ArrayList<JobElement>();

    private String jobXmlName;

    /**
     * Steps and Flows that inherit from a parent (i.e., has a non-null parent attribute). They can be top-level
     * job elements or nested under other job elements.
     */
    final List<InheritableJobElement> inheritingJobElements = new ArrayList<InheritableJobElement>();

    Job(final String id) {
        super(id);
    }

    /**
     * Gets the job's {@code restartable} attribute value as string.
     *
     * @return the job's {@code restartable} attribute value as string
     */
    public String getRestartable() {
        return restartable;
    }

    /**
     * Gets the job's {@code restartable} attribute value as boolean.
     *
     * @return the job's {@code restartable} attribute value as boolean
     */
    public boolean getRestartableBoolean() {
        return (restartable == null || restartable.isEmpty()) ? true : Boolean.parseBoolean(restartable);
    }

    /**
     * Sets the job's {@code restartable} attribute string value.
     *
     * @param restartable the job's {@code restartable} attribute string value
     */
    void setRestartable(final String restartable) {
        if (restartable != null) {
            this.restartable = restartable;
        }
    }

    /**
     * Sets {@link #jobXmlName} only if it differs from job id.
     *
     * @param jobXmlName the base name of the job xml file, which may be different from job id attribute
     *
     * @since 1.1.0
     */
    public void setJobXmlName(final String jobXmlName) {
        this.jobXmlName = jobXmlName;
    }

    /**
     * Gets {@link #jobXmlName}, which may be null.
     *
     * @return a non-null value if it differs from {@link #id}; otherwise return null.
     *
     * @since 1.1.0
     */
    public String getJobXmlName() {
        return jobXmlName;
    }

    /**
     * Gets the list of job elements, such as steps, decisions, flows or splits, contained in this job.
     *
     * @return the list of job elements contained in this job
     */
    public List<JobElement> getJobElements() {
        return jobElements;
    }

    /**
     * Adds a job element to the job's job element list.
     *
     * @param jobElement the job element to add, which may be a step, decision, flow, or split
     */
    void addJobElement(final JobElement jobElement) {
        jobElements.add(jobElement);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Job job = (Job) o;

        if (!id.equals(job.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public List<Transition> getTransitionElements() {
        throw new IllegalStateException();
    }

    @Override
    public void addTransitionElement(final Transition transition) {
        throw new IllegalStateException();
    }

    /**
     * Gets all direct or indirect job elements in this job that inherit from a parent (ie, has a non-null
     * parent attribute). They are either steps or flows, and can be top-level job elements or nested under other
     * job elements.
     *
     * @return all direct or indirect inheriting job elements (steps and flows)
     */
    public List<InheritableJobElement> getInheritingJobElements() {
        return inheritingJobElements;
    }
}
