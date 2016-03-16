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
    private final JobSchedule jobSchedule;

    public JobScheduleTask(final JobSchedule jobSchedule) {
        this.jobSchedule = jobSchedule;
    }

    @Override
    public void run() {
        final JobScheduleConfig config = jobSchedule.getJobScheduleConfig();
        if (config.jobExecutionId > 0) {
            jobSchedule.addJobExecutionIds(
                JobScheduler.getJobOperator().restart(config.jobExecutionId, config.jobParameters));
        } else {
            jobSchedule.addJobExecutionIds(
                JobScheduler.getJobOperator().start(config.jobName, config.jobParameters));
        }
    }
}
