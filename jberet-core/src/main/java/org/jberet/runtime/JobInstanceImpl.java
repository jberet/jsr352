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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jberet.job.model.Job;
import org.jberet.metadata.ApplicationMetaData;
import org.jberet.repository.ApplicationAndJobName;

public final class JobInstanceImpl implements JobInstance {
    private long id;
    Job unsubstitutedJob;
    ApplicationAndJobName applicationAndJobName;

    private List<JobExecution> jobExecutions = new ArrayList<JobExecution>();

    private ApplicationMetaData applicationMetaData;

    public JobInstanceImpl(Job unsubstitutedJob, ApplicationAndJobName applicationAndJobName) {
        this.applicationAndJobName = applicationAndJobName;
        this.unsubstitutedJob = unsubstitutedJob;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getJobName() {
        return applicationAndJobName.jobName;
    }

    public String getApplicationName() {
        return applicationAndJobName.appName;
    }

    @Override
    public long getInstanceId() {
        return this.id;
    }

    public ApplicationMetaData getApplicationMetaData() {
        return applicationMetaData;
    }

    public void setApplicationMetaData(ApplicationMetaData applicationMetaData) {
        this.applicationMetaData = applicationMetaData;
    }

    public List<JobExecution> getJobExecutions() {
        return Collections.unmodifiableList(this.jobExecutions);
    }

    public void addJobExecution(JobExecution jobExecution) {
        this.jobExecutions.add(jobExecution);
    }
}
