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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorSchedulerImpl extends JobScheduler {
    protected final ScheduledExecutorService executorService;

    private final ConcurrentMap<String, JobSchedule> schedules;

    private final AtomicInteger ids = new AtomicInteger(1);

    public ExecutorSchedulerImpl() {
        this(null);
    }

    public ExecutorSchedulerImpl(final ConcurrentMap<String, JobSchedule> schedules) {
        this(schedules, null);
    }

    public ExecutorSchedulerImpl(final ConcurrentMap<String, JobSchedule> schedules,
                                 final ScheduledExecutorService executorService) {
        this.schedules = schedules == null ?
                new ConcurrentHashMap<String, JobSchedule>() : schedules;
        this.executorService = executorService == null ?
                Executors.newSingleThreadScheduledExecutor() : executorService;
    }

    @Override
    public JobSchedule schedule(final JobScheduleConfig scheduleConfig) {
        final JobSchedule jobSchedule = new JobSchedule(String.valueOf(ids.getAndIncrement()), scheduleConfig);
        final JobScheduleTask task = new JobScheduleTask(jobSchedule);

        final Future<?> future;
        if (scheduleConfig.interval <= 0 && scheduleConfig.delay <= 0) {
            future = executorService.schedule(task, scheduleConfig.initialDelay, timeUnit);
        } else if (scheduleConfig.interval > 0) {
            future = executorService.scheduleAtFixedRate(
                    task, scheduleConfig.initialDelay, scheduleConfig.interval, timeUnit);
        } else {
            future = executorService.scheduleWithFixedDelay(
                    task, scheduleConfig.initialDelay, scheduleConfig.delay, timeUnit);
        }
        jobSchedule.setFuture(future);
        schedules.put(jobSchedule.getId(), jobSchedule);
        return jobSchedule;
    }

    @Override
    public List<JobSchedule> getJobSchedules() {
        final List<JobSchedule> result = new ArrayList<JobSchedule>();
        for (final JobSchedule e : schedules.values()) {
            final JobSchedule.Status status = e.getStatus();
            if (status != JobSchedule.Status.CANCELLED && status != JobSchedule.Status.DONE) {
                final Future<?> future = e.getFuture();
                if (future != null) {
                    if (future.isCancelled()) {
                        e.setStatus(JobSchedule.Status.CANCELLED);
                    } else if (future.isDone()) {
                        e.setStatus(JobSchedule.Status.DONE);
                    }
                }
            }
            result.add(e);
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public boolean cancel(final String scheduleId) {
        boolean result = false;
        final JobSchedule jobSchedule = schedules.get(scheduleId);
        if (jobSchedule != null) {
            final Future<?> future = jobSchedule.getFuture();
            if (future != null) {
                result = future.cancel(false);
                if (result) {
                    jobSchedule.setStatus(JobSchedule.Status.CANCELLED);
                }
            }
        }
        return result;
    }

    @Override
    public JobSchedule getJobSchedule(final String scheduleId) {
        final JobSchedule jobSchedule = schedules.get(scheduleId);
        if (jobSchedule != null) {
            final JobSchedule.Status status = jobSchedule.getStatus();
            if (status != JobSchedule.Status.CANCELLED && status != JobSchedule.Status.DONE) {
                final Future<?> future = jobSchedule.getFuture();
                if (future != null) {
                    if (future.isCancelled()) {
                        jobSchedule.setStatus(JobSchedule.Status.CANCELLED);
                    } else if (future.isDone()) {
                        jobSchedule.setStatus(JobSchedule.Status.DONE);
                    }
                }
            }
        }
        return jobSchedule;
    }

    @Override
    public String toString() {
        return  getClass().getName() + "{executorService=" + executorService + '}';
    }
}
