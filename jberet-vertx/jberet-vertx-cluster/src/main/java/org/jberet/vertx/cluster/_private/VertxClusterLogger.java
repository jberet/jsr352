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

package org.jberet.vertx.cluster._private;

import java.io.Serializable;

import org.jberet.vertx.cluster.VertxPartitionInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 74500, max = 74999)
public interface VertxClusterLogger extends BasicLogger {
    VertxClusterLogger LOGGER = Logger.getMessageLogger(VertxClusterLogger.class, "org.jberet.vertx-cluster");

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 74500, value = "Created clustered Vertx %s")
    void createdClusteredVertx(Object vertx);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 74501, value = "Received partition info at remote node: %s")
    void receivedPartitionInfo(VertxPartitionInfo partitionInfo);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 74502, value = "Received partition execution result for partition %s")
    void receivedPartitionResult(int partitionId);@LogMessage(level = Logger.Level.INFO)

    @Message(id = 74503, value =
    "Received request to stop partition execution [job execution %s, step name %s, step execution %s, partition %s]")
    void receivedStopRequest(long jobExecutionId, String stepName, long stepExecutionId, int partitionId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 74504, value = "Sending partition execution collector data for step execution %s: %s")
    void sendCollectorData(long stepExecutionId, Serializable collectorData);

}