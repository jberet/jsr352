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

import java.util.Collection;

public class TimerServiceSchedulerImpl extends JobScheduler {

    @Override
    public JobScheduleInfo schedule(final JobScheduleInfo scheduleInfo) {
        final JobScheduleTask task = new JobScheduleTask(scheduleInfo);

        return scheduleInfo;
    }

    @Override
    public Collection<JobScheduleInfo> getJobSchedules() {
        return null;
    }

    @Override
    public boolean cancel(final JobScheduleInfo scheduleInfo) {
        return false;
    }
}
