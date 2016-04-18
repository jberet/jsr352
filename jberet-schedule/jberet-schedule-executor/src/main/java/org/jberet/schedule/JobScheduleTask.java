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

/**
 * A task that is submitted to the job scheduler, and when the schedule matures,
 * starts the job or restarts the job execution based on {@link JobSchedule}.
 *
 * @see JobSchedule
 * @see JobScheduler
 * @since 1.3.0
 */
class JobScheduleTask implements Runnable {
    private final JobSchedule jobSchedule;

    /**
     * Creates {@code JobScheduleTask} with {@link JobSchedule} passed in.
     * @param jobSchedule
     */
    JobScheduleTask(final JobSchedule jobSchedule) {
        this.jobSchedule = jobSchedule;
    }

    /**
     * Runs the task by starting the job or restarting the job execution,
     * and saving the new job execution id in {@link JobSchedule}.
     */
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
