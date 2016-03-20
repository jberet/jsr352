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

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jberet.schedule._private.ScheduleExecutorLogger;
import org.jberet.schedule._private.ScheduleExecutorMessages;

public abstract class JobScheduler {
    protected static final TimeUnit timeUnit = TimeUnit.MINUTES;

    protected static final String TIMER_SCHEDULER_LOOKUP = "java:module/TimerSchedulerBean";
    protected static final String MANAGED_EXECUTOR_SERVICE_LOOKUP =
            "java:comp/DefaultManagedScheduledExecutorService";

    private static volatile JobScheduler jobScheduler;

    protected JobScheduler() {
    }

    public static JobScheduler getJobScheduler() {
        return getJobScheduler(null, null);
    }

    public static JobScheduler getJobScheduler(final Class<? extends JobScheduler> schedulerType) {
        return getJobScheduler(schedulerType, null);
    }

    public static JobScheduler getJobScheduler(final ConcurrentMap<String, JobSchedule> schedules) {
        return getJobScheduler(null, schedules);
    }

    public static JobScheduler getJobScheduler(final Class<? extends JobScheduler> schedulerType,
                                               final ConcurrentMap<String, JobSchedule> schedules) {
        JobScheduler result = jobScheduler;
        if (result == null) {
            synchronized (JobScheduler.class) {
                result = jobScheduler;
                if (result == null) {
                    if (schedulerType != null) {
                        try {
                            jobScheduler = result = schedulerType.newInstance();
                        } catch (final Throwable e) {
                            throw ScheduleExecutorMessages.MESSAGES.failToCreateJobScheduler(e, schedulerType);
                        }
                    } else {
                        InitialContext ic = null;
                        try {
                            ic = new InitialContext();
                            try {
                                jobScheduler = result = (JobScheduler) ic.lookup(TIMER_SCHEDULER_LOOKUP);
                            } catch (final NamingException e) {
                                try {
                                    final ScheduledExecutorService mexe =
                                            (ScheduledExecutorService) ic.lookup(MANAGED_EXECUTOR_SERVICE_LOOKUP);
                                    jobScheduler = result = new ExecutorSchedulerImpl(schedules, mexe);
                                } catch (final NamingException e2) {
                                    jobScheduler = result = new ExecutorSchedulerImpl(schedules);
                                }
                            }
                        } catch (final NamingException e) {
                            //log warning
                            jobScheduler = result = new ExecutorSchedulerImpl();
                        } finally {
                            if (ic != null) {
                                try {
                                    ic.close();
                                } catch (final NamingException e2) {
                                    //ignore it
                                }
                            }
                        }
                    }
                    ScheduleExecutorLogger.LOGGER.createdJobScheduler(result);
                }
            }
        }
        return result;
    }

    public static JobOperator getJobOperator() {
        return Holder.jobOperator;
    }

    private static class Holder {
        private static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    }



    public abstract JobSchedule schedule(final JobScheduleConfig scheduleConfig);

    public abstract List<JobSchedule> getJobSchedules();

    public abstract boolean cancel(final String scheduleId);

    public abstract JobSchedule getJobSchedule(final String scheduleId);
}
