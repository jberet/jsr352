/*
 * Copyright (c) 2016-2017 Red Hat, Inc. and/or its affiliates.
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
     * @see #getJobScheduler(Class, ConcurrentMap, String)
     */
    public static JobScheduler getJobScheduler() {
        return getJobScheduler(null, null, null);
    }

    /**
     * Gets the job scheduler, specifying scheduler type,
     * {@code ConcurrentMap<String, JobSchedule>} for storing all job schedules, and the lookup
     * name of {@code ManagedScheduledExecutorService} resource.
     *
     * This method determines which type of job scheduler to use as follows:
     * <ul>
     *   <li>If {@code schedulerType} is specified, instantiate the specified type.
     *   <li>Else if lookup of {@value #TIMER_SCHEDULER_LOOKUP} succeeds,
     *      use the job scheduler obtained from that lookup.
     *   <li>Else if lookup of the resource specified by {@code managedScheduledExecutorServiceLookup} succeeds,
     *      creates {@link ExecutorSchedulerImpl} with the executor from that lookup.
     *   <li>Else if lookup of {@value #MANAGED_EXECUTOR_SERVICE_LOOKUP} succeeds,
     *      creates {@link ExecutorSchedulerImpl} with the executor from that lookup.
     *   <li>Else creates {@link ExecutorSchedulerImpl}.
     * </ul>
     *
     * @param schedulerType fully-qualified class name of job scheduler type
     * @param schedules {@code ConcurrentMap<String, JobSchedule>} for storing all job schedules
     * @param managedScheduledExecutorServiceLookup lookup name of {@code ManagedScheduledExecutorService} resource
     * @return job scheduler
     */
    public static JobScheduler getJobScheduler(final Class<? extends JobScheduler> schedulerType,
                                               final ConcurrentMap<String, JobSchedule> schedules,
                                               final String managedScheduledExecutorServiceLookup) {
        JobScheduler result = jobScheduler;
        if (result == null) {
            synchronized (JobScheduler.class) {
                result = jobScheduler;
                if (result == null) {
                    if (schedulerType != null) {
                        try {
                            jobScheduler = result = schedulerType.newInstance();
                            ScheduleExecutorLogger.LOGGER.createdJobScheduler(result, null);
                        } catch (final Throwable e) {
                            throw ScheduleExecutorMessages.MESSAGES.failToCreateJobScheduler(e, schedulerType);
                        }
                    } else {
                        InitialContext ic = null;
                        try {
                            ic = new InitialContext();
                            try {
                                jobScheduler = result = (JobScheduler) ic.lookup(TIMER_SCHEDULER_LOOKUP);
                                ScheduleExecutorLogger.LOGGER.createdJobScheduler(result, TIMER_SCHEDULER_LOOKUP);
                            } catch (final NamingException e) {
                                ScheduledExecutorService mexe;
                                if (managedScheduledExecutorServiceLookup != null) {
                                    try {
                                        mexe = (ScheduledExecutorService) ic.lookup(managedScheduledExecutorServiceLookup);
                                        jobScheduler = result = new ExecutorSchedulerImpl(schedules, mexe);
                                        ScheduleExecutorLogger.LOGGER.createdJobScheduler(result, managedScheduledExecutorServiceLookup);
                                    } catch (final NamingException e2) {
                                        ScheduleExecutorLogger.LOGGER.failToLookupManagedScheduledExecutorService(managedScheduledExecutorServiceLookup);
                                        mexe = (ScheduledExecutorService) ic.lookup(MANAGED_EXECUTOR_SERVICE_LOOKUP);
                                        jobScheduler = result = new ExecutorSchedulerImpl(schedules, mexe);
                                        ScheduleExecutorLogger.LOGGER.createdJobScheduler(result, MANAGED_EXECUTOR_SERVICE_LOOKUP);
                                    }
                                } else {
                                    mexe = (ScheduledExecutorService) ic.lookup(MANAGED_EXECUTOR_SERVICE_LOOKUP);
                                    jobScheduler = result = new ExecutorSchedulerImpl(schedules, mexe);
                                    ScheduleExecutorLogger.LOGGER.createdJobScheduler(result, MANAGED_EXECUTOR_SERVICE_LOOKUP);
                                }
                            }
                        } catch (final NamingException e) {
                            jobScheduler = result = new ExecutorSchedulerImpl(schedules);
                            ScheduleExecutorLogger.LOGGER.createdJobScheduler(result, null);
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
     * Deletes a job schedule by its id. The default implementation of this method
     * just calls {@link #cancel(String)} method.
     *
     * @param scheduleId the schedule id to delete
     *
     * @since 1.3.0.Beta7
     */
    public void delete(final String scheduleId) {
        cancel(scheduleId);
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
