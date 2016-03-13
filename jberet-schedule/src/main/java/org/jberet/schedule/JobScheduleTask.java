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

public class JobScheduleTask implements Runnable {
    private final JobScheduleInfo jobScheduleInfo;

    public JobScheduleTask(final JobScheduleInfo jobScheduleInfo) {
        this.jobScheduleInfo = jobScheduleInfo;
    }

    @Override
    public void run() {
        if (jobScheduleInfo.jobExecutionId > 0) {
            jobScheduleInfo.result =
                JobScheduler.getJobOperator().restart(jobScheduleInfo.jobExecutionId, jobScheduleInfo.jobParameters);
        } else {
            jobScheduleInfo.result =
                JobScheduler.getJobOperator().start(jobScheduleInfo.jobName, jobScheduleInfo.jobParameters);
        }
    }
}
