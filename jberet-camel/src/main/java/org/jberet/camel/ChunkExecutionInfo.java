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

import java.util.List;

public final class ChunkExecutionInfo {
    private final long jobExecutionId;

    private final String jobName;

    private final long stepExecutionId;

    private final String stepName;

    private final Exception exception;

    private final Object item;

    private final Object result;

    private final List<Object> items;

    ChunkExecutionInfo(final long jobExecutionId,
                       final String jobName,
                       final long stepExecutionId,
                       final String stepName,
                       final Object item,
                       final Object result,
                       final List<Object> items,
                       final Exception exception) {
        this.jobExecutionId = jobExecutionId;
        this.jobName = jobName;
        this.stepExecutionId = stepExecutionId;
        this.stepName = stepName;
        this.item = item;
        this.result = result;
        this.items = items;
        this.exception = exception;
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }

    public long getStepExecutionId() {
        return stepExecutionId;
    }

    public Exception getException() {
        return exception;
    }

    public Object getItem() {
        return item;
    }

    public Object getResult() {
        return result;
    }

    public List<Object> getItems() {
        return items;
    }

    public String getJobName() {
        return jobName;
    }

    public String getStepName() {
        return stepName;
    }

    @Override
    public String toString() {
        return "ChunkExecutionInfo{" +
                "jobExecutionId=" + jobExecutionId +
                ", jobName='" + jobName + '\'' +
                ", stepExecutionId=" + stepExecutionId +
                ", stepName='" + stepName + '\'' +
                ", exception=" + exception +
                ", item=" + item +
                ", result=" + result +
                ", items=" + items +
                '}';
    }
}
