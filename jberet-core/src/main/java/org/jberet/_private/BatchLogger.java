/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet._private;

import java.sql.Connection;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 1, max = 599)
public interface BatchLogger extends BasicLogger {
    BatchLogger LOGGER = org.jboss.logging.Logger.getMessageLogger(BatchLogger.class, "org.jberet");

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 1, value = "Failed to run batchlet %s")
    void failToRunBatchlet(@Cause Throwable e, Object o);

    @Message(id = 2, value = "A step cannot contain both chunk type and batchlet type: %s")
    @LogMessage(level = Logger.Level.WARN)
    void cannotContainBothChunkAndBatchlet(String stepId);

    @Message(id = 3, value = "A concrete step must contain either a chunk or batchlet type: %s")
    @LogMessage(level = Logger.Level.WARN)
    void stepContainsNoChunkOrBatchlet(String stepId);

    @Message(id = 4, value = "Unrecognized property category: %s, variable name: %s in property value: %s")
    @LogMessage(level = Logger.Level.WARN)
    void unrecognizedPropertyReference(String category, String variableName, String propVal);

    @Message(id = 5, value = "Invalid exception filter class '%s'")
    @LogMessage(level = Logger.Level.WARN)
    void invalidExceptionClassFilter(@Cause Throwable cause, String cls);

    @Message(id = 6, value = "The job: %s already exists in the job repository and will not be added.")
    @LogMessage(level = Logger.Level.TRACE)
    void jobAlreadyExists(String jobId);

    @Message(id = 7, value = "Failed to run job %s, %s, %s")
    @LogMessage(level = Logger.Level.ERROR)
    void failToRunJob(@Cause Throwable e, String jobName, String stepName, Object artifact);

    @Message(id = 8, value = "Possible syntax errors in property: %s")
    @LogMessage(level = Logger.Level.WARN)
    void possibleSyntaxErrorInProperty(String prop);

    @Message(id = 9, value = "A decision cannot be the first element: %s")
    @LogMessage(level = Logger.Level.WARN)
    void decisionCannotBeFirst(String decisionId);

    @Message(id = 10, value = "Could not resolve expression because: %s")
    @LogMessage(level = Logger.Level.DEBUG)
    void unresolvableExpression(String message);

    @Message(id = 11, value = "Failed to stop the job %s, %s, %s")
    @LogMessage(level = Logger.Level.WARN)
    void failToStopJob(@Cause Throwable cause, String jobName, String stepName, Object additionalInfo);

    @Message(id = 12, value = "Failed to clone %s when running job [%s] and step [%s]")
    @LogMessage(level = Logger.Level.WARN)
    void failToClone(@Cause Throwable cause, Object original, String jobName, String stepName);

    @Message(id = 13, value = "Failed to destroy artifact %s")
    @LogMessage(level = Logger.Level.WARN)
    void failToDestroyArtifact(@Cause Throwable cause, Object artifact);

    @Message(id = 14, value = "Tables created for batch job repository with DDL file %s")
    @LogMessage(level = Logger.Level.DEBUG)
    void tableCreated(String ddlFile);

    @Message(id = 15, value = "Adding ddl entry for batch job repository:%n %s")
    @LogMessage(level = Logger.Level.DEBUG)
    void addDDLEntry(String ddlContent);

    @Message(id = 16, value = "Failed to close %s: %s")
    @LogMessage(level = Logger.Level.WARN)
    void failToClose(@Cause Throwable cause, Class<?> resourceType, Object obj);

    @Message(id = 17, value = "Persisted %s with id %s")
    @LogMessage(level = Logger.Level.DEBUG)
    void persisted(Object obj, long id);

    @Message(id = 18, value = "Could not find the original step execution to restart.  Current step execution id: %s, step name: %s")
    @LogMessage(level = Logger.Level.WARN)
    void couldNotFindOriginalStepToRestart(long currentStepExecutionId, String stepName);

    @Message(id = 19, value = "Encountered errors when creating batch job repository tables.")
    @LogMessage(level = Logger.Level.WARN)
    void errorWhenCreatingTable(@Cause Throwable cause);

    @Message(id = 20, value = "Failed to get database product name from connection %s")
    @LogMessage(level = Logger.Level.WARN)
    void failToGetDatabaseProductName(@Cause Throwable cause, Connection connection);

    @Message(id = 21, value = "About to initialize batch job repository with ddl-file: %s for database %s")
    @LogMessage(level = Logger.Level.INFO)
    void ddlFileAndDatabaseProductName(String ddlFile, String databaseProductName);

    @Message(id = 22, value = "%s is not implemented for local transactions")
    @LogMessage(level = Logger.Level.TRACE)
    void notImplementedOnLocalTx(String methodName);

}