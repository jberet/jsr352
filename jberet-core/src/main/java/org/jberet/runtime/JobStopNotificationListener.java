/*
 * Copyright (c) 2014-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime;

/**
 * An interface to be implemented by classes that wish to be notified when its job is requested to stop.
 */
public interface JobStopNotificationListener {
    /**
     * Invoked when the current job is requested to stop.
     *
     * @param jobExecutionId the job execution id requested to stop
     */
    void stopRequested(long jobExecutionId);
}
