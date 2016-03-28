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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import javax.batch.operations.BatchRuntimeException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jberet.schedule._private.ScheduleExecutorMessages;

@Singleton()
public class TimerSchedulerBean extends JobScheduler {
    //TODO need to handle non-persistent timer.
    private static final boolean isPersistent = true;

    @Resource
    private TimerService timerService;

    @Override
    public JobSchedule schedule(final JobScheduleConfig scheduleConfig) {
        Timer timer = null;
        final JobSchedule jobSchedule = new JobSchedule(null, scheduleConfig);
        if (scheduleConfig.initialDelay > 0) {
            if (scheduleConfig.interval > 0) {
                timer = timerService.createIntervalTimer(toMillis(scheduleConfig.initialDelay),
                        toMillis(scheduleConfig.interval),
                        new TimerConfig(jobSchedule, isPersistent));
            } else if (scheduleConfig.delay > 0) {
                timer = timerService.createIntervalTimer(toMillis(scheduleConfig.initialDelay),
                        toMillis(scheduleConfig.delay),
                        new TimerConfig(jobSchedule, isPersistent));
            } else {
                timer = timerService.createSingleActionTimer(toMillis(scheduleConfig.initialDelay),
                        new TimerConfig(jobSchedule, isPersistent));
            }
        } else if (scheduleConfig.scheduleExpression != null) {
            timer = timerService.createCalendarTimer(scheduleConfig.scheduleExpression,
                    new TimerConfig(jobSchedule, isPersistent));
        } else {
            throw ScheduleExecutorMessages.MESSAGES.invalidJobScheduleConfig(scheduleConfig);
        }

        jobSchedule.setId(getTimerId(timer));
        return jobSchedule;
    }

    @Override
    public List<JobSchedule> getJobSchedules() {
        final List<JobSchedule> result = new ArrayList<JobSchedule>();
        final Collection<Timer> timers = timerService.getTimers();
        for (final Timer t : timers) {
            Serializable info = t.getInfo();
            if (info instanceof JobSchedule) {
                result.add((JobSchedule) info);
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public boolean cancel(final String scheduleId) {
        final Collection<Timer> timers = timerService.getTimers();
        for (final Timer t : timers) {
            if (scheduleId.equals(getTimerId(t))) {
                t.cancel();
                return true;
            }
        }
        return false;
    }

    @Override
    public JobSchedule getJobSchedule(final String scheduleId) {
        final Collection<Timer> timers = timerService.getTimers();
        for (final Timer e : timers) {
            final JobSchedule js = (JobSchedule) e.getInfo();
            if (scheduleId.equals(js.getId())) {
                return js;
            }
        }
        return null;
    }

    @Timeout
    protected void timeout(final Timer timer) {
        final JobSchedule jobSchedule = (JobSchedule) timer.getInfo();
        final JobScheduleConfig scheduleConfig = jobSchedule.getJobScheduleConfig();
        if (scheduleConfig.getJobExecutionId() > 0) {
            jobSchedule.addJobExecutionIds(
            JobScheduler.getJobOperator().restart(scheduleConfig.jobExecutionId, scheduleConfig.jobParameters));
        } else {
            jobSchedule.addJobExecutionIds(
            JobScheduler.getJobOperator().start(scheduleConfig.jobName, scheduleConfig.jobParameters));
        }
    }

    private static long toMillis(final long t) {
        return JobScheduler.timeUnit.toMillis(t);
    }

    /**
     * Gets timer id from its string representation, e.g.,
     * <p/>
     * [id=ea4f6ae1-4a73-4480-bf89-d1f361c7d56a timedObjectId=...]
     * <p/>
     * Improve it when appropriate timer API is available.
     *
     * @param timer the timer
     * @return timer id
     */
    private static String getTimerId(final Timer timer) {
        final String s = timer.toString();
        int start = s.indexOf("id=");
        if (start < 0) {
            throw new BatchRuntimeException("Failed to get timer id: " + timer);
        }
        start += 3;
        final int end = s.indexOf(' ', start);
        return end > 0 ? s.substring(start, end) : s.substring(start);
    }
}
