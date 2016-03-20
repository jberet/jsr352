/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.schedule._private;

import org.jberet.schedule.JobScheduler;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 72500, max = 72999)
public interface ScheduleExecutorLogger extends BasicLogger {
    ScheduleExecutorLogger LOGGER = Logger.getMessageLogger(
            ScheduleExecutorLogger.class, "org.jberet.schedule-executor");

    @Message(id = 72500, value = "Created JobScheduler: %s")
    @LogMessage(level = Logger.Level.INFO)
    void createdJobScheduler(JobScheduler jobScheduler);

}