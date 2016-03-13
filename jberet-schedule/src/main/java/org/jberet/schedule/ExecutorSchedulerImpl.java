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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorSchedulerImpl extends JobScheduler {
    protected final ScheduledExecutorService executorService;
    private final ConcurrentMap<JobScheduleInfo, Future<?>> schedules;

    public ExecutorSchedulerImpl() {
        this(null);
    }

    public ExecutorSchedulerImpl(final ConcurrentMap<JobScheduleInfo, Future<?>> schedules) {
        this(schedules, null);
    }

    protected ExecutorSchedulerImpl(final ConcurrentMap<JobScheduleInfo, Future<?>> schedules,
                                    final ScheduledExecutorService executorService) {
        this.schedules = schedules == null ?
                new ConcurrentHashMap<JobScheduleInfo, Future<?>>() : schedules;
        this.executorService = executorService == null ?
                Executors.newSingleThreadScheduledExecutor() : executorService;
    }

    @Override
    public JobScheduleInfo schedule(final JobScheduleInfo scheduleInfo) {
        final JobScheduleTask task = new JobScheduleTask(scheduleInfo);

        final Future<?> future;
        if (scheduleInfo.period == 0) {
            future = executorService.schedule(task, scheduleInfo.delay, timeUnit);
        } else {
            future = executorService.scheduleWithFixedDelay(
                    task, scheduleInfo.delay, scheduleInfo.period, timeUnit);
        }
        schedules.put(scheduleInfo, future);
        return scheduleInfo;
    }

    @Override
    public Collection<JobScheduleInfo> getJobSchedules() {
        return schedules.keySet();
    }

    @Override
    public boolean cancel(final JobScheduleInfo scheduleInfo) {
        final Future<?> future = schedules.get(scheduleInfo);
        return future != null ? future.cancel(false) : false;
    }
}
