/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.util;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.mybatch.runtime.context.AbstractContext;

@MessageLogger(projectCode = "mybatch")
public interface BatchLogger extends BasicLogger {
    BatchLogger LOGGER = org.jboss.logging.Logger.getMessageLogger(BatchLogger.class, BatchLogger.class.getPackage().getName());

    @Message(id = 1, value = "Failed to create artifact with ref name %s")
    IllegalStateException failToCreateArtifact(@Cause Throwable e, String ref);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 2,
            value = "Usage: java -classpath ... -Dkey1=val1 ... org.mybatch.Main jobXML%nThe following application args are invalid:%n%s")
    void mainUsage(List<String> args);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 3, value = "Failed to run batchlet %s")
    void failToRunBatchlet(@Cause Throwable e, Object o);

    @Message(id = 4, value = "Failed to get job xml file for job %s")
    JobStartException failToGetJobXml(@Cause Throwable e, String jobName);

    @Message(id = 5, value = "Failed to parse and bind XML for job %s")
    JobStartException failToParseBindJobXml(@Cause Throwable e, String jobName);

    @Message(id = 6, value = "Failed to parse and bind batch XML %s")
    JobStartException failToParseBindBatchXml(@Cause Throwable e, String batchXML);

    @Message(id = 7, value = "Failed to process batch application metadata for job %s")
    JobStartException failToProcessMetaData(@Cause Throwable e, String jobName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 8, value = "Failed to write batch artifact xml file")
    void failToWriteBatchXml(@Cause Throwable e);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 9, value = "Failed to identify batch artifact")
    void failToIdentifyArtifact(@Cause Throwable e);

    @Message(id = 10, value = "A step cannot contain both chunk type and batchlet type: %s")
    @LogMessage(level = Logger.Level.WARN)
    void cannotContainBothChunkAndBatchlet(String stepId);

    @Message(id = 11, value = "A concrete step must contain either a chunk or batchlet type: %s")
    @LogMessage(level = Logger.Level.WARN)
    void stepContainsNoChunkOrBatchlet(String stepId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 12, value = "Submitted batchlet task %s in thread %s")
    void submittedBatchletTask(String b, Thread th);

    @Message(id = 13, value = "No method matches the annotation %s in artifact %s")
    IllegalStateException noMethodMatchingAnnotation(Class<? extends Annotation> ann, Object artifact);

    @Message(id = 14, value = "No job execution with id %s")
    NoSuchJobExecutionException noSuchJobExecution(long executionId);

    @Message(id = 15, value = "Unrecognized property category: %s, variable name: %s in property value: %s")
    @LogMessage(level = Logger.Level.WARN)
    void unrecognizedPropertyReference(String category, String variableName, String propVal);

    @Message(id = 16, value = "Invalid exception filter class '%s'")
    @LogMessage(level = Logger.Level.WARN)
    void invalidExceptionClassFilter(String cls);

    @Message(id = 17, value = "The job: %s already exists in the job repository and cannot be added again.")
    @LogMessage(level = Logger.Level.WARN)
    void jobAlreadyExists(String jobId);

    @Message(id = 18, value = "Failed to run artifact %s, %s")
    @LogMessage(level = Logger.Level.ERROR)
    void failToRunJob(@Cause Throwable e, String name, Object artifact);

    @Message(id = 19, value = "Unrecognizable job element: %s in job: %s")
    IllegalStateException unrecognizableJobElement(String jobElementName, String jobName);

    @Message(id = 20, value = "Cycle detected in property reference: %s")
    IllegalStateException cycleInPropertyReference(List<String> referringExpressions);

    @Message(id = 21, value = "Possible syntax errors in property: %s")
    @LogMessage(level = Logger.Level.WARN)
    void possibleSyntaxErrorInProperty(String prop);

    @Message(id = 22, value = "The step %s would form a loopback in sequence: %s")
    IllegalStateException loopbackStep(String stepId, String executedSteps);

    @Message(id = 23, value = "The requested batch operation %s is not supported in %s")
    IllegalStateException batchOperationNotSupported(String op, AbstractContext context);

    @Message(id = 24, value = "Cycles detected in job element inheritance: %s")
    JobStartException cycleInheritance(String inheritingElements);

    @Message(id = 25, value = "Job execution %s is running and cannot be abandoned.")
    JobExecutionIsRunningException jobExecutionIsRunningException(long jobExecutionId);

    @Message(id = 26, value = "Job execution %s has already completed and cannot be restarted.")
    JobExecutionAlreadyCompleteException jobExecutionAlreadyCompleteException(long jobExecutionId);

    @Message(id = 27, value = "Failed to restart job execution %s, which had batch status %s.")
    JobRestartException jobRestartException(long jobExecutionId, BatchStatus previousStatus);

    @Message(id = 28, value = "Job execution %s is not the most recent execution of job instance %s.")
    JobExecutionNotMostRecentException jobExecutionNotMostRecentException(long jobExecutionId, long jobInstanceId);

    @Message(id = 29, value = "Job execution %s has batch status %s, and is not running.")
    JobExecutionNotRunningException jobExecutionNotRunningException(long jobExecutionId, BatchStatus batchStatus);


}