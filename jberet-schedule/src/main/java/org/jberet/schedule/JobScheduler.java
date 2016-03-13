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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public abstract class JobScheduler {
    protected static final TimeUnit timeUnit = TimeUnit.MINUTES;

    protected static final String TIMER_SERVICE_LOOKUP = "java:comp/TimerService";
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

    public static JobScheduler getJobScheduler(final ConcurrentMap<JobScheduleInfo, Future<?>> schedules) {
        return getJobScheduler(null, schedules);
    }

    public static JobScheduler getJobScheduler(final Class<? extends JobScheduler> schedulerType,
                                               final ConcurrentMap<JobScheduleInfo, Future<?>> schedules) {
        JobScheduler result = jobScheduler;
        if (result == null) {
            synchronized (JobScheduler.class) {
                result = jobScheduler;
                if (result == null) {
                    if (schedulerType != null) {
                        try {
                            return jobScheduler = schedulerType.newInstance();
                        } catch (final Throwable e) {
                            throw new BatchRuntimeException(e);
                        }
                    } else {
                        InitialContext ic = null;
                        try {
                            ic = new InitialContext();
                            try {
                                ic.lookup(TIMER_SERVICE_LOOKUP);
                                return jobScheduler = new TimerServiceSchedulerImpl();
                            } catch (final NamingException e) {
                                try {
                                    final ScheduledExecutorService mexe =
                                            (ScheduledExecutorService) ic.lookup(MANAGED_EXECUTOR_SERVICE_LOOKUP);
                                    return jobScheduler = new ManagedExecutorSchedulerImpl(schedules, mexe);
                                } catch (final NamingException e2) {
                                    return jobScheduler = new ExecutorSchedulerImpl(schedules);
                                }
                            }
                        } catch (final NamingException e) {
                            //log warning
                            return jobScheduler = new ExecutorSchedulerImpl();
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

    public abstract JobScheduleInfo schedule(final JobScheduleInfo scheduleInfo);

    public abstract Collection<JobScheduleInfo> getJobSchedules();

    public abstract boolean cancel(final JobScheduleInfo scheduleInfo);
}
