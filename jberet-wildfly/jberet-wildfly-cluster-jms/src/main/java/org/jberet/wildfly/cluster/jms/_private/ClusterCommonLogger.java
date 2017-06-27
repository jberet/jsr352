/*
 * Copyright (c) 2012-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.wildfly.cluster.jms._private;

import java.io.Serializable;
import javax.batch.runtime.BatchStatus;

import org.jberet.spi.PartitionInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 75500, max = 75999)
public interface ClusterCommonLogger extends BasicLogger {
    ClusterCommonLogger LOGGER = Logger.getMessageLogger(ClusterCommonLogger.class, "org.jberet.wildfly-cluster");

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 75501, value = "Received partition info at remote node: %s")
    void receivedPartitionInfo(PartitionInfo partitionInfo);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 75502,
            value = "Received partition execution result: job execution %s, step execution %s, partition %s, partition status %s")
    void receivedPartitionResult(long jobExecutionId, long stepExecutionId, int partitionId, BatchStatus batchStatus);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 75503, value =
            "Received request to stop partition execution [job execution %s, step name %s, step execution %s, partition %s]")
    void receivedStopRequest(long jobExecutionId, String stepName, long stepExecutionId, int partitionId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 75504,
            value = "Sending partition execution collector data for step execution %s partition %s: %s")
    void sendCollectorData(long stepExecutionId, int partitionId, Serializable collectorData);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 75505, value = "Encountered problem when closing resource")
    void problemClosingResource(@Cause Throwable throwable);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 75506, value = "Looked up JNDI resource %s: %s")
    void lookupResource(String name, Object result);


}