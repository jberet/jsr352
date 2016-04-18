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

/**
 * This class defines operations for a job scheduler, and also static methods
 * for obtaining an instance of {@code JobScheduler}.
 *
 * @see JobSchedule
 * @see JobScheduleConfig
 * @since 1.3.0
 */
public abstract class JobScheduler {
    /**
     * Convenience empty string array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Feature name for persistent job schedule.
     */
    public static final String PERSISTENT = "persistent";

    /**
     * Feature name for calendar-based job schedule.
     */
    public static final String CALENDAR = "calendar";

    /**
     * Time unit for job schedule.
     */
    protected static final TimeUnit timeUnit = TimeUnit.MINUTES;

    /**
     * Lookup name for EJB-Timer-based job scheduler implementation.
     */
    protected static final String TIMER_SCHEDULER_LOOKUP = "java:module/TimerSchedulerBean";

    /**
     * Lookup name for the managed scheduled executor service.
     */
    protected static final String MANAGED_EXECUTOR_SERVICE_LOOKUP =
            "java:comp/DefaultManagedScheduledExecutorService";

    /**
     * The job scheduler instance.
     */
    private static volatile JobScheduler jobScheduler;

    /**
     * Default no-arg constructor.
     */
    protected JobScheduler() {
    }

    /**
     * Gets the job scheduler without passing any parameters.
     * @return job scheduler
     *
     * @see #getJobScheduler(Class, ConcurrentMap)
     */
    public static JobScheduler getJobScheduler() {
        return getJobScheduler(null, null);
    }

    /**
     * Gets the job scheduler of the specified {@code schedulerType}.
     * @param schedulerType fully-qualified class name of job scheduler
     * @return job scheduler of the specified
     *
     * @see #getJobScheduler(Class, ConcurrentMap)
     */
    public static JobScheduler getJobScheduler(final Class<? extends JobScheduler> schedulerType) {
        return getJobScheduler(schedulerType, null);
    }

    /**
     * Gets the job scheduler, specifying a {@code ConcurrentMap<String, JobSchedule>}
     * for storing all job schedules.
     *
     * @param schedules {@code ConcurrentMap<String, JobSchedule>} for storing all job schedules
     * @return job scheduler
     *
     * @see #getJobScheduler(Class, ConcurrentMap)
     */
    public static JobScheduler getJobScheduler(final ConcurrentMap<String, JobSchedule> schedules) {
        return getJobScheduler(null, schedules);
    }

    /**
     * Gets the job scheduler, specifying both the scheduler type and
     * {@code ConcurrentMap<String, JobSchedule>} for storing all job schedules.
     *
     * This method determines which type of job scheduler to use as follows:
     * <ul>
     *   <li>If {@code schedulerType} is specified, instantiate the specified type.
     *   <li>Else if lookup of {@value #TIMER_SCHEDULER_LOOKUP} succeeds,
     *      use the job scheduler obtained from that lookup.
     *   <li>Else if lookup of {@value #MANAGED_EXECUTOR_SERVICE_LOOKUP} succeeds,
     *      creates {@link ExecutorSchedulerImpl} with the executor from that lookup.
     *   <li>Else creates {@link ExecutorSchedulerImpl}.
     * </ul>
     *
     * @param schedulerType rully-qualified class name of job scheduler type
     * @param schedules {@code ConcurrentMap<String, JobSchedule>} for storing all job schedules
     * @return job scheduler
     */
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

    /**
     * Convenience method for getting {@code JobOperator}.
     * @return {@code JobOperator}
     */
    public static JobOperator getJobOperator() {
        return Holder.jobOperator;
    }


    private static class Holder {
        private static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    }


    /**
     * Gets the features supported by the current job scheduler.
     * @return feature names as a string array
     *
     * @see #EMPTY_STRING_ARRAY
     * @see #PERSISTENT
     * @see #CALENDAR
     */
    public String[] getFeatures() {
        return EMPTY_STRING_ARRAY;
    }

    /**
     * submits a job schedule specified with the job schedule config.
     * @param scheduleConfig job schedule config
     * @return the job schedule resulting from the submission
     */
    public abstract JobSchedule schedule(final JobScheduleConfig scheduleConfig);

    /**
     * Gets all job schedules known to the scheduler.
     * Some implementation may keep the job schedule record after its expiration
     * or cancellation, and some may just remove them immediately.
     *
     * @return all job schedules
     */
    public abstract List<JobSchedule> getJobSchedules();

    /**
     * Cancels a job schedule by its id.
     * @param scheduleId the schedule id to cancel
     * @return true if cancelled successfully; false otherwise
     */
    public abstract boolean cancel(final String scheduleId);

    /**
     * Gets the job schedule by its id.
     * @param scheduleId id of the job schedule to retrieve
     * @return the job schedule matching the specified id
     */
    public abstract JobSchedule getJobSchedule(final String scheduleId);

}
