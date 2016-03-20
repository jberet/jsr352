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

import javax.batch.operations.BatchRuntimeException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 72000, max = 72499)
public interface ScheduleExecutorMessages {
    ScheduleExecutorMessages MESSAGES = Messages.getBundle(ScheduleExecutorMessages.class);

    @Message(id = 72000, value = "Failed to create JobScheduler of type %s")
    BatchRuntimeException failToCreateJobScheduler(@Cause Throwable th, Class<?> schedulerType);

}