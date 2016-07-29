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

package org.jberet.camel;

/**
 * @since 1.3.0
 */
public final class EventType {
    private EventType() {
    }

    /**
     * The key of the message header to indicate whether the event is for
     * before job or after job execution.
     */
    public static final String EVENT_TYPE = "eventType";
    /**
     * The value of the message header to indicate that the event is for
     * before job execution.
     */
    public static final String BEFORE_JOB = "beforeJob";
    /**
     * The value of the message header to indicate that the event is for
     * after job execution.
     */
    public static final String AFTER_JOB = "afterJob";

    /**
     * The value of the message header to indicate that the event is for
     * before step execution.
     */
    public static final String BEFORE_STEP = "beforeStep";

    /**
     * The value of the message header to indicate that the event is for
     * after step execution.
     */
    public static final String AFTER_STEP = "afterStep";

}
