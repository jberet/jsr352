package org.jberet._private;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.xml.stream.Location;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 600, max = 999)
public interface BatchMessages {

    BatchMessages MESSAGES = Messages.getBundle(BatchMessages.class);

    @Message(id = 600, value = "Failed to create artifact with ref name %s.  Ensure CDI beans.xml is present and batch.xml, if any, is configured properly.")
    IllegalStateException failToCreateArtifact(@Cause Throwable e, String ref);

    @Message(id = 601, value = "Failed to get job xml file for job %s")
    JobStartException failToGetJobXml(@Cause Throwable e, String jobName);

    JobStartException failToGetJobXml(String jobName);

    @Message(id = 602, value = "Failed to parse and bind XML for job %s")
    JobStartException failToParseJobXml(@Cause Throwable e, String jobName);

    @Message(id = 603, value = "Failed to parse batch XML %s")
    JobStartException failToParseBatchXml(@Cause Throwable e, String batchXML);

    @Message(id = 604, value = "No job execution with id %s")
    NoSuchJobExecutionException noSuchJobExecution(long executionId);

    @Message(id = 605, value = "Unrecognizable job element: %s in job: %s")
    IllegalStateException unrecognizableJobElement(String jobElementName, String jobName);

    @Message(id = 606, value = "Cycle detected in property reference: %s")
    BatchRuntimeException cycleInPropertyReference(List<String> referringExpressions);

    @Message(id = 607, value = "The step %s would form a loopback in sequence: %s")
    IllegalStateException loopbackStep(String stepId, String executedSteps);

    @Message(id = 608, value = "Job execution %s is running and cannot be abandoned.")
    JobExecutionIsRunningException jobExecutionIsRunningException(long jobExecutionId);

    @Message(id = 609, value = "Job execution %s has already completed and cannot be restarted.")
    JobExecutionAlreadyCompleteException jobExecutionAlreadyCompleteException(long jobExecutionId);

    @Message(id = 610, value = "Failed to restart job execution %s, which had batch status %s.")
    JobRestartException jobRestartException(long jobExecutionId, BatchStatus previousStatus);

    @Message(id = 611, value = "Job execution %s is not the most recent execution of job instance %s.")
    JobExecutionNotMostRecentException jobExecutionNotMostRecentException(long jobExecutionId, long jobInstanceId);

    @Message(id = 612, value = "Job execution %s has batch status %s, and is not running.")
    JobExecutionNotRunningException jobExecutionNotRunningException(long jobExecutionId, BatchStatus batchStatus);

    @Message(id = 613, value = "The step %s has started %s times and reached its start limit %s")
    BatchRuntimeException stepReachedStartLimit(String stepName, int startLimit, int startCount);

    @Message(id = 614, value = "Invalid chunk checkpoint-policy %s.  It must be either item or custom.")
    BatchRuntimeException invalidCheckpointPolicy(String checkpointPolicy);

    @Message(id = 616, value = "Invalid chunk item-count %s.  It must be greater than 0.")
    BatchRuntimeException invalidItemCount(int itemCount);

    @Message(id = 617, value = "checkpoint-algorithm element is missing in step %s.  It is required for custom checkpoint-policy.")
    BatchRuntimeException checkpointAlgorithmMissing(String stepName);

    @Message(id = 618, value = "Failed to inject value %s into field %s, because the field type %s is not supported for property injection.")
    BatchRuntimeException unsupportedFieldType(String v, Field f, Class<?> t);

    @Message(id = 619, value = "Failed to inject value %s into field %s")
    BatchRuntimeException failToInjectProperty(@Cause Throwable th, String v, Field f);

    // @Message(id = 620, value = "Unrecognized job repository type %s")
    // BatchRuntimeException unrecognizedJobRepositoryType(String v);

    @Message(id = 621, value = "Failed to look up datasource by jndi name %s.")
    BatchRuntimeException failToLookupDataSource(@Cause Throwable cause, String dataSourceName);

    @Message(id = 622, value = "Failed to obtain connection from %s")
    BatchRuntimeException failToObtainConnection(@Cause Throwable cause, Object connectionSource);

    @Message(id = Message.INHERIT, value = "Failed to obtain connection from %s, %s")
    BatchRuntimeException failToObtainConnection(@Cause Throwable cause, Object connectionSource, Object props);

    @Message(id = 623, value = "Failed to load sql properties %s")
    BatchRuntimeException failToLoadSqlProperties(@Cause Throwable cause, String sqlFile);

    @Message(id = 624, value = "Failed to create tables for batch job repository database product name %s with DDL %s")
    BatchRuntimeException failToCreateTables(@Cause Throwable cause, String databaseProductName, String ddlFile);

    @Message(id = 625, value = "Failed to load ddl file %s")
    BatchRuntimeException failToLoadDDL(String ddlFile);

    @Message(id = 626, value = "Failed to run %s")
    BatchRuntimeException failToRunQuery(@Cause Throwable cause, String sql);

    @Message(id = 627, value = "Unexpected XML element '%s' at location %s")
    BatchRuntimeException unexpectedXmlElement(String element, Location location);

    @Message(id = 628, value = "Failed to get XML attribute '%s' at location %s")
    BatchRuntimeException failToGetAttribute(String attributeName, Location location);

    @Message(id = 629, value = "Cannot have both next attribute and next element at location %s  Next attribute is already set to %s")
    BatchRuntimeException cannotHaveBothNextAttributeAndElement(Location location, String nextAttributeValue);

    @Message(id = 630, value = "The job instance: %s already exists in the job repository and cannot be added again.")
    BatchRuntimeException jobInstanceAlreadyExists(long jobInstanceId);

    @Message(id = 631, value = "The job execution: %s already exists in the job repository and cannot be added again.")
    BatchRuntimeException jobExecutionAlreadyExists(long jobExecutionId);

    @Message(id = 632, value = "The specified job with the name %s does not exist.")
    NoSuchJobException noSuchJobException(String jobName);

    @Message(id = 633, value = "Invalid JBeret configuration property %s = %s.")
    BatchRuntimeException invalidConfigProperty(@Cause Throwable throwable, String key, String value);

    @Message(id = 634, value = "Cycles detected in job element inheritance: %s")
    JobStartException cycleInheritance(String inheritingElements);

    @Message(id = 635, value = "Could not find implementation of %s")
    IllegalStateException implementationNotFound(Class<?> c);

    @Message(id = 636, value = "Cannot have both script element and ref attribute: %s")
    BatchRuntimeException cannotHaveBothScriptAndRef(String ref);

    @Message(id = 637, value = "Invalid script attributes: type = %s, src = %s")
    BatchRuntimeException invalidScriptAttributes(String scriptType, String scriptSrc);

    @Message(id = 638, value = "Failed to get the script content from %s")
    BatchRuntimeException failToGetScriptContent(@Cause Throwable th, String scriptSrc);

    @Message(id = 639, value = "A job repository is required")
    BatchRuntimeException jobRepositoryRequired();

    @Message(id = 640, value = "A BatchEnvironment implementation could not be found. Please ensure the SPI has been implemented and is on the class path")
    BatchRuntimeException batchEnvironmentNotFound();

    @Message(id = 641, value = "The script is not Invocable: %s")
    BatchRuntimeException scriptNotInvocable(String scriptContent);

    @Message(id = 642, value = "Failed to create cache manager: %s")
    BatchRuntimeException failToCreateCacheManager(@Cause Throwable th, String description);

    @Message(id = 643, value = "Failed to get next id number: %s")
    BatchRuntimeException failToGetNextId(@Cause Throwable th, String key);

    @Message(id = 644, value = "Execution of split %s timed out after %s seconds")
    BatchRuntimeException splitExecutionTimeout(String splitId, long timeoutSeconds);

    @Message(id = 645, value = "No job instance %s")
    NoSuchJobInstanceException noSuchJobInstance(String jobInstanceValue);

    @Message(id = 646, value = "Failed to restart the job with name: %s, execution id: %s, because the job is configured not restartable")
    JobRestartException unrestartableJob(String jobName, long jobExecutionId);

    @Message(id = 647, value = "Restarting job execution %s, job name %s, batch status %s, restart mode %s, but it seems the original execution is still alive.")
    JobRestartException restartRunningExecution(long executionId, String jobName, BatchStatus previousStatus, String restartMode);

    @Message(id = 648, value = "Restarting job execution %s, job name %s, batch status %s, but restart mode %s is invalid. Valid values are %s")
    JobRestartException invalidRestartMode(long executionId, String jobName, BatchStatus previousStatus, String restartMode, List<String> validRestartMode);

    @Message(id = 649, value = "%s cannot be null")
    IllegalArgumentException nullVar(String name);

    @Message(id = 650, value = "%s id %s already exists")
    BatchRuntimeException idAlreadyExists(String jobElementType, String id);

    @Message(id = 651, value = "The requested permits (%d) is greater than the maximum number of permits (%d) allowed in the thread pool.")
    IllegalStateException insufficientPermits(int requestedPermits, int maxPermits);

    @Message(id = 652, value = "Failed to serialize: %s")
    BatchRuntimeException failedToSerialize(@Cause Throwable cause, Serializable value);

    @Message(id = 653, value = "Failed to deserialize: %s")
    BatchRuntimeException failedToDeserialize(@Cause Throwable cause, Serializable value);

    @Message(id = 654, value = "Failed to get JdbcRepository.")
    BatchRuntimeException failedToGetJdbcRepository(@Cause Throwable cause);

    @Message(id = 655, value = "ClassLoader (%s) is already registered to a job operator context")
    IllegalArgumentException classLoaderAlreadyRegistered(ClassLoader classLoader);

}
