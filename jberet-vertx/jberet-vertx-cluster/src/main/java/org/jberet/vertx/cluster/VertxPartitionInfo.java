/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.cluster;

import java.io.Serializable;

import org.jberet.job.model.Step;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.PartitionExecutionImpl;

public class VertxPartitionInfo implements Serializable {
    private static final long serialVersionUID = -8906466052166191853L;

    public static final String PARTITION_QUEUE = "jberet.partition";
    public static final String COLLECTOR_QUEUE = "jberet.collector";
    public static final String STOP_REQUEST_TOPIC = "jberet.stop";

    PartitionExecutionImpl partitionExecution;
    Step step;
    JobExecutionImpl jobExecution;

    public VertxPartitionInfo(final PartitionExecutionImpl partitionExecution,
                              final Step step,
                              final JobExecutionImpl jobExecution) {
        this.partitionExecution = partitionExecution;
        this.step = step;
        this.jobExecution = jobExecution;
    }

    /**
     * Returns the name of the queue for sending and receiving partition execution
     * collector data.
     *
     * @param stepExecutionId the step execution id used to uniquely identify the step execution
     * @return the name of the queue for sending and receiving partition execution collector data
     */
    public static String getCollectorQueueName(final long stepExecutionId) {
        return COLLECTOR_QUEUE + stepExecutionId;
    }

    public static String getStopRequestTopicName(final long jobExecutionId) {
        return STOP_REQUEST_TOPIC + jobExecutionId;
    }

    @Override
    public String toString() {
        return "VertxPartitionInfo{" +
                "partitionExecution=" + partitionExecution.getPartitionId() +
                ", step=" + step.getId() +
                ", stepExecution=" + partitionExecution.getStepExecutionId() +
                ", jobExecution=" + jobExecution.getExecutionId() +
                ", jobName=" + jobExecution.getJobName() +
                '}';
    }
}
