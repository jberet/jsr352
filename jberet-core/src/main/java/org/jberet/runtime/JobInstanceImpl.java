/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;

public final class JobInstanceImpl implements JobInstance, Serializable {

    private static final long serialVersionUID = -933284750735124427L;

    private long id;
    private long version;
    private String jobName;
    private String applicationName;
    Job unsubstitutedJob;

    private final List<JobExecution> jobExecutions = new ArrayList<JobExecution>();

    public JobInstanceImpl(final Job unsubstitutedJob, final ApplicationAndJobName applicationAndJobName) {
        this.jobName = applicationAndJobName.jobName;
        this.applicationName = applicationAndJobName.appName;
        this.unsubstitutedJob = unsubstitutedJob;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    @Override
    public long getInstanceId() {
        return this.id;
    }

    public List<JobExecution> getJobExecutions() {
        return Collections.unmodifiableList(this.jobExecutions);
    }

    public void addJobExecution(final JobExecution jobExecution) {
        this.jobExecutions.add(jobExecution);
    }

    public Job getUnsubstitutedJob() {
        return unsubstitutedJob;
    }

    public void setUnsubstitutedJob(final Job unsubstitutedJob) {
        this.unsubstitutedJob = unsubstitutedJob;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final JobInstanceImpl that = (JobInstanceImpl) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
