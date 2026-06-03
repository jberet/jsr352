/*
 * Copyright (c) 2014-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
