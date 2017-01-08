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

/**
 * This class represents batch chunk execution information.
 *
 * @see CamelChunkListener
 * @since 1.3.0
 */
public final class ChunkExecutionInfo {
    /**
     * Current job execution id.
     */
    private final long jobExecutionId;

    /**
     * Job name or id, as specified as {@code job id} attribute in job XML file.
     */
    private final String jobName;

    /**
     * Current step execution id.
     */
    private final long stepExecutionId;

    /**
     * Step name, as specified as {@code step id} attribute in job XML file.
     */
    private final String stepName;

    /**
     * Any exception during chunk execution.
     */
    private final Exception exception;

    /**
     * The item currently being processed. It may be null if the current
     * execution stage (e.g., item reading or writing) does not involve an item.
     */
    private final Object item;

    /**
     * The result object from processing. It may be null if the current
     * execution stage (e.g., item reading or writing) does not involve a result.
     */
    private final Object result;

    /**
     * The items being written by the current item writer. It may be null if
     * the current execution stage (e.g., item reading or processing) does not
     * involve such items.
     */
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

    /**
     * Gets the current job execution id.
     *
     * @return the current job execution id
     */
    public long getJobExecutionId() {
        return jobExecutionId;
    }

    /**
     * Gets the current step execution id.
     * @return the current step execution id
     */
    public long getStepExecutionId() {
        return stepExecutionId;
    }

    /**
     * Gets the exception occurred during chunk execution.
     * @return exception during chunk exception, or null if no exception
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Gets the item currently being processed. It may be null if the current
     * execution stage (e.g., item reading or writing) does not involve an item.
     * @return item currently being processed
     */
    public Object getItem() {
        return item;
    }

    /**
     * Gets the result object from processing. It may be null if the current
     * execution stage (e.g., item reading or writing) does not involve a result.
     * @return the result object from processing
     */
    public Object getResult() {
        return result;
    }

    /**
     * Gets the items being written by the current item writer. It may be null if
     * the current execution stage (e.g., item reading or processing) does not
     * involve such items.
     * @return the items being written by the writer
     */
    public List<Object> getItems() {
        return items;
    }

    /**
     * Gets the job name or id, as specified as {@code job id} attribute in job XML file.
     * @return the job or id
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Gets the step name, as specified as {@code step id} attribute in job XML file.
     * @return the step name
     */
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
