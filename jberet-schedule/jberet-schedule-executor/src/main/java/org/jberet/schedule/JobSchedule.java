/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.schedule;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a job schedule. Instances of this class may be transferred
 * during remote REST API invocations.
 *
 * @see JobScheduleConfig
 * @since 1.3.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JobSchedule implements Serializable, Comparable<JobSchedule> {
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
     * instantiation if possible, but in some implementations of
     * {@link JobScheduler}, it may have to be set afterwards.
     */
    private String id;

    /**
     * How the job schedule is configured, usually passed from the client
     * side.
     */
    private final JobScheduleConfig jobScheduleConfig;

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
     * there is at most 1 element; for repeatable job schedules, there can
     * be many elements.
     */
    private List<Long> jobExecutionIds = new CopyOnWriteArrayList<Long>();

    /**
     * The {@code java.util.concurrent.Future} object from submitting the
     * job schedule. It can be used for status checking and cancellation.
     * Some {@link JobScheduler} implementations may not need or support
     * {@code Future}, and in that case, this field is not used.
     */
    private transient Future<?> future;

    /**
     * Default no-arg constructor.
     */
    public JobSchedule() {
        this(null, null);
    }

    /**
     * Constructs {@code JobSchedule} with id and {@code JobScheduleConfig}.
     *
     * @param id job schedule id
     * @param jobScheduleConfig job schedule config
     */
    public JobSchedule(final String id, final JobScheduleConfig jobScheduleConfig) {
        this.id = id;
        this.jobScheduleConfig = jobScheduleConfig;
        this.createTime = new Date();
    }

    /**
     * Gets the job schedule id.
     * @return job schedule id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the job schedule config, which is typically passed in when creating
     * {@code JobSchedule}.
     *
     * @return job schedule config
     */
    public JobScheduleConfig getJobScheduleConfig() {
        return jobScheduleConfig;
    }

    /**
     * Gets the job schedule status.
     * @return job schedule status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets all ids of job executions that have realized from this job schedule.
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
        return createTime.compareTo(o.createTime);
    }

    void addJobExecutionIds(final long jobExecutionId) {
        jobExecutionIds.add(jobExecutionId);
    }

    void setStatus(final Status status) {
        this.status = status;
    }

    Future<?> getFuture() {
        return future;
    }

    void setFuture(final Future<?> future) {
        this.future = future;
    }

    void setId(final String id) {
        this.id = id;
    }
}
