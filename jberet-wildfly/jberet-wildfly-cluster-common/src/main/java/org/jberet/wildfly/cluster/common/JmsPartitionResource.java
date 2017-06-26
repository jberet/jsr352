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

package org.jberet.wildfly.cluster.common;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jberet.wildfly.cluster.common.org.jberet.wildfly.cluster.common._private.ClusterCommonLogger;
import org.jberet.wildfly.cluster.common.org.jberet.wildfly.cluster.common._private.ClusterCommonMessages;

public final class JmsPartitionResource {
    public static final String CONNECTION_FACTORY = "jms/connectionFactory";
    public static final String PARTITION_QUEUE = "jms/partitionQueue";
    public static final String STOP_REQUEST_TOPIC = "jms/stopRequestTopic";

    public static final String MESSAGE_JOB_EXECUTION_ID_KEY = "jobExecutionId";
    public static final String MESSAGE_STEP_EXECUTION_ID_KEY = "stepExecutionId";
    public static final String MESSAGE_TYPE_KEY = "type";
    public static final String MESSAGE_TYPE_PARTITION = "P";
    public static final String MESSAGE_TYPE_RESULT = "R";

    private final Context namingContext;

    public JmsPartitionResource() {
        try {
            namingContext = new InitialContext();
        } catch (NamingException e) {
            throw ClusterCommonMessages.MESSAGES.failedToNewNamingContext(e);
        }
    }

    public <T> T lookUp(final String name) {
        String s = "java:comp/env/" + name;
        try {
            T result = (T) namingContext.lookup(s);
            ClusterCommonLogger.LOGGER.lookupResource(s, result);
            return result;
        } catch (NamingException e) {
            throw ClusterCommonMessages.MESSAGES.failedToLookup(e, s);
        }
    }

    public Queue getPartitionQueue() {
        return lookUp(PARTITION_QUEUE);
    }

    public Topic getStopRequestTopic() {
        return lookUp(STOP_REQUEST_TOPIC);
    }

    public ConnectionFactory getConnectionFactory() {
        return lookUp(CONNECTION_FACTORY);
    }

    public static String getMessageSelector(final String messageType, final long stepExecutionId) {
        return stepExecutionId > 0 ?
                String.format("%s = '%s' AND %s = %s",
                        MESSAGE_TYPE_KEY, messageType, MESSAGE_STEP_EXECUTION_ID_KEY, stepExecutionId) :
                String.format("%s = '%s'", MESSAGE_TYPE_KEY, messageType);
    }

    public static String getMessageSelector(final long jobExecutionId) {
        return String.format("%s = %s", MESSAGE_JOB_EXECUTION_ID_KEY, jobExecutionId);
    }

    public void close() {
        if (namingContext != null) {
            try {
                namingContext.close();
            } catch (NamingException e) {
                ClusterCommonLogger.LOGGER.problemClosingResource(e);
            }
        }
    }

    public static void closeJmsContext(final JMSContext jmsContext) {
        if (jmsContext != null) {
            try {
                jmsContext.close();
            } catch (Exception e) {
                ClusterCommonLogger.LOGGER.problemClosingResource(e);
            }
        }
    }
}
