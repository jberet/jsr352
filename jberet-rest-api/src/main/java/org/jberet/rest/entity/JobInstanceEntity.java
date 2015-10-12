/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.entity;

import java.io.Serializable;
import java.util.List;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = "instanceId, jobName, jobExecutions")
public final class JobInstanceEntity implements JobInstance, Serializable {
    private static final long serialVersionUID = 2427272964201557394L;

    private long instanceId;
    private String jobName;
    private JobExecutionEntity[] jobExecutions;

    //    private int numberOfJobExecutions;

    public JobInstanceEntity() {
    }

    public JobInstanceEntity(final JobInstance jobInstance, final List<JobExecution> jobExecutions) {
        this.instanceId = jobInstance.getInstanceId();
        this.jobName = jobInstance.getJobName();
        this.jobExecutions = JobExecutionEntity.fromJobExecutions(jobExecutions);
    }

    public long getInstanceId() {
        return instanceId;
    }

    public String getJobName() {
        return jobName;
    }

    public JobExecutionEntity[] getJobExecutions() {
        return jobExecutions;
    }
}
