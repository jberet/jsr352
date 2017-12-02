/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.rest;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.vertx.core.shareddata.Shareable;

/**
 * Represents a job schedule.
 *
 * @since 1.3.0.Beta7
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JobSchedule implements Serializable, Comparable<JobSchedule>, Shareable {
    private static final long serialVersionUID = 5759369754976526021L;

    /**
     * Statuses of {@code JobSchedule}.
     */
    public enum Status {
        /**
         * The {@code JobSchedule} has been submitted, and has not been
         * cancelled or finished. The job schedule may be pending, running,
         * or being idle between 2 runs in case of repeatable job schedule.
         */
        SCHEDULED,

        /**
         * The {@code JobSchedule} has been cancelled.
         */
        CANCELLED,

        /**
         * The {@code JobSchedule} has finished all the scheduled work,
         * without being cancelled.
         */
        DONE,

        /**
         * Unknown status.
         */
        UNKNOWN
    }

    /**
     * id of the job schedule. It should be initialized as part of the
     * instantiation if possible, but in some cases, it may have to be set afterwards.
     * Defaults to -1, since in Vert.x 0 is a valid timer id (the first timer).
     */
    private long id = -1;

    /**
     * The time the job schedule is created.
     */
    private final Date createTime;

    /**
     * The default status is {@code SCHEDULED}.
     */
    private Status status = Status.SCHEDULED;

    /**
     * A list to save all job execution ids. For single-action job schedule,
     * there is at most 1 element; for periodic job schedules, there can
     * be many elements.
     */
    private List<Long> jobExecutionIds = new CopyOnWriteArrayList<Long>();

    /**
     * The job XML name for the job schedule to start the job.
     * Either {@code jobName} or {@code jobExecutionId} should be
     * specified, but not both.
     */
    private String jobName;

    /**
     * The id of a job execution for the job schedule to restart it.
     * Either {@code jobName} or {@code jobExecutionId} should be
     * specified, but not both.
     */
    private long jobExecutionId;

    /**
     * The job parameters for starting the job or restarting the job execution.
     */
    private Properties jobParameters;

    /**
     * The delay (in minutes) of the job schedule.
     */
    private long delay;

    /**
     * Default no-arg constructor.
     */
    public JobSchedule() {
        this(0);
    }

    public JobSchedule(final long id) {
        this.id = id;
        this.createTime = new Date();
    }

    /**
     * Gets the job schedule id.
     *
     * @return job schedule id
     */
    public synchronized long getId() {
        return id;
    }

    /**
     * Gets the job schedule status.
     *
     * @return job schedule status
     */
    public synchronized Status getStatus() {
        return status;
    }

    /**
     * Gets all ids of job executions that have realized from this job schedule.
     *
     * @return all job execution ids
     */
    public List<Long> getJobExecutionIds() {
        return Collections.unmodifiableList(jobExecutionIds);
    }

    /**
     * Compares another job schedule to this one, based on their create time.
     *
     * @param o the other job schedule
     * @return the result of comparing their create time
     */
    @Override
    public int compareTo(final JobSchedule o) {
        final long diff = id - o.getId();
        return diff < 0 ? -1 : diff > 0 ?  1 : 0;
    }

    void addJobExecutionIds(final long jobExecutionId) {
        jobExecutionIds.add(jobExecutionId);
    }

    synchronized void setStatus(final Status status) {
        this.status = status;
    }

    synchronized void setId(final long id) {
        this.id = id;
    }

    synchronized public String getJobName() {
        return jobName;
    }

    public synchronized void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    public synchronized long getJobExecutionId() {
        return jobExecutionId;
    }

    public synchronized void setJobExecutionId(final long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public synchronized Properties getJobParameters() {
        return jobParameters;
    }

    public synchronized void setJobParameters(final Properties jobParameters) {
        this.jobParameters = jobParameters;
    }

    public synchronized long getDelay() {
        return delay;
    }

    public synchronized void setDelay(final long delay) {
        this.delay = delay;
    }
}
