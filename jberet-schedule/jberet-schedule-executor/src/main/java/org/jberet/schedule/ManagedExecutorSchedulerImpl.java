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

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

public class ManagedExecutorSchedulerImpl extends ExecutorSchedulerImpl {
    public ManagedExecutorSchedulerImpl() {
        this(null, null);
    }

    public ManagedExecutorSchedulerImpl(final ConcurrentMap<String, JobSchedule> schedules,
                                        final ScheduledExecutorService executorService) {
        super(schedules, executorService);
    }
}
