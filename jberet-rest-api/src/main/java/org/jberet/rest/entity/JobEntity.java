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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = "jobName, numberOfJobInstances, numberOfRunningJobExecutions")
public final class JobEntity implements Serializable {
    private static final long serialVersionUID = -7252231657018200476L;
    private String jobName;
    private int numberOfJobInstances;
    private int numberOfRunningJobExecutions;

    public JobEntity() {
    }

    public JobEntity(final String jobName, final int numberOfJobInstances,
                     final int numberOfRunningJobExecutions) {
        this.jobName = jobName;
        this.numberOfJobInstances = numberOfJobInstances;
        this.numberOfRunningJobExecutions = numberOfRunningJobExecutions;
    }

    public String getJobName() {
        return jobName;
    }

    public int getNumberOfJobInstances() {
        return numberOfJobInstances;
    }

    public int getNumberOfRunningJobExecutions() {
        return numberOfRunningJobExecutions;
    }
}
