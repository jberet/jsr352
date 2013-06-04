/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jberet.job.Job;
import org.jberet.metadata.ApplicationMetaData;
import org.jberet.repository.ApplicationAndJobName;

public final class JobInstanceImpl implements JobInstance {
    private long id;
    Job unsubstitutedJob;
    ApplicationAndJobName applicationAndJobName;

    private List<JobExecution> jobExecutions = new ArrayList<JobExecution>();

    private ApplicationMetaData applicationMetaData;

    public JobInstanceImpl(long id, Job unsubstitutedJob, ApplicationAndJobName applicationAndJobName) {
        this.id = id;
        this.applicationAndJobName = applicationAndJobName;
        this.unsubstitutedJob = unsubstitutedJob;
    }

    @Override
    public String getJobName() {
        return applicationAndJobName.jobName;
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
