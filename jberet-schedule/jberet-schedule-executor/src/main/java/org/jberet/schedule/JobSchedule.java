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
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JobSchedule implements Serializable, Comparable<JobSchedule> {
    private static final long serialVersionUID = 5759369754976526021L;

    public enum Status {
        SCHEDULED,
        CANCELLED,
        DONE,
        UNKNOWN
    }

    private String id;

    private final JobScheduleConfig jobScheduleConfig;

    private final Date createTime;

    private Status status = Status.SCHEDULED;

    private List<Long> jobExecutionIds = new CopyOnWriteArrayList<Long>();

    @XmlTransient
    private transient Future<?> future;

    public JobSchedule() {
        this(null, null);
    }

    public JobSchedule(final String id, final JobScheduleConfig jobScheduleConfig) {
        this.id = id;
        this.jobScheduleConfig = jobScheduleConfig;
        this.createTime = new Date();
    }

    public String getId() {
        return id;
    }

    public JobScheduleConfig getJobScheduleConfig() {
        return jobScheduleConfig;
    }

    public Status getStatus() {
        return status;
    }

    public List<Long> getJobExecutionIds() {
        return Collections.unmodifiableList(jobExecutionIds);
    }

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
