/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.batch.operations;
import java.util.List;
import java.util.Set;
import java.util.Properties; 
import javax.batch.operations.exception.JobExecutionIsRunningException;
import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobInstanceAlreadyCompleteException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.operations.exception.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
public interface JobOperator {
/**
* Returns a set of all job names known to the batch runtime.
*
* @return a set of job names.
*/
Set<String> getJobNames();

/**
* Returns number of instances of a job with a particular name (id).
*
* @param jobName
* specifies the name of the job.
* @return count of instances of the named job.
* @throws NoSuchJobException
*/
int getJobInstanceCount(String jobName) throws NoSuchJobException;
/**
* Returns all instanceIds belonging to a job with a particular name.
*
* @param jobName
* identifies the job name.
* @param start
* identifies the relative starting number to return from the
* maximal list of job instances.
* @param count
* identifies the number of instance ids to return from the
* starting position of the maximal list of job instances.
* @return list of instance ids
* @throws NoSuchJobException
*/
List<Long> getJobInstanceIds(String jobName, int start, int count)
throws NoSuchJobException;
/**
* Returns instanceIds for all running jobs across all instances of a job
* with a particular name.
*
* @param jobName
* identifies the job name.
* @return a Set of instanceIds
* @throws NoSuchJobException
*/
Set<Long> getRunningInstanceIds(String jobName) throws NoSuchJobException;
/**
* Returns all executionIds belonging to a particular job instance.
*
* @param instanceId
* identifies the job instance
* @return List of executionIds
* @throws NoSuchJobInstanceException
*/
List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException;
/**
* Returns job parameters for specified execution. These are the key/value
* pairs specified when the instance was started or restarted.
*
* @param executionId
* identifies the execution.
* @return a Properties object containing the key/value job parameter pairs.
* @throws NoSuchJobExecutionException
*/
Properties getParameters(long executionId)
throws NoSuchJobExecutionException;
/**
* Creates a new job instance and starts the first execution of that
* instance.
*
* @param job
* specifies the Job XML describing the job.
* @param jobParameters
* specifies the keyword/value pairs for property override and
* substitution in Job XML.
* @return instanceId of the new job instance.
* @throws JobStartException
*/
Long start(String job, Properties jobParameters) throws JobStartException;
/**
* Restarts a failed or stopped job instance.
*
* @param instanceId
* belonging to the instance to restart. The execution that
* restarts is the most recent execution to run.
* @param jobParameters
* specify replacement job parameters for the job restart. The
* replacement add to and/or override the original job parameters
* that were specified when the instance was originally started.
* @return new executionId
* @throws JobInstanceAlreadyCompleteException
* @throws NoSuchJobExecutionException
* @throws NoSuchJobException
* @throws JobRestartException
*/
Long restart(long instanceId, Properties jobParameters)
throws JobInstanceAlreadyCompleteException,
NoSuchJobExecutionException, NoSuchJobException,
JobRestartException;
/**
* Request a running execution stops. *
*
* @param instanceId
* specifies the instance to stop (the currently running
* execution is stopped)
* @throws NoSuchJobExecutionException
* @throws JobExecutionNotRunningException
*/
void stop(long instanceId) throws NoSuchJobExecutionException,
JobExecutionNotRunningException;
/**
* Set batch status to ABANDONED. The instance must not be
* running.
*
* @param instanceId
* specifies the instance to mark abandoned
* @throws NoSuchJobExecutionException * @throws JobExecutionIsRunningException
*/
void abandon(long instanceId) throws NoSuchJobExecutionException, JobExecutionIsRunningException;
/**
* Return the job instance for the specified job instance id
*
* @param instanceId
* specifies the requested job instance
* @return job instance
*/
JobInstance getJobInstance(long instanceId);
/**
* Return all job executions belonging to the specified job instance
*
* @param jobInstance
* specifies the job instance
* @return list of job executions
*/
List<JobExecution> getJobExecutions(long instanceId);
/**
* Return job execution for specified execution id
*
* @param executionId
* specifies the requested job execution
* @return job execution
*/
JobExecution getJobExecution(long executionId);
/**
* Return step execution for specified execution
*
* @param jobExecutionId
* specifies the job execution
* @param stepExecutionId
* specifies the step belonging to that execution
* @return step execution
*/
StepExecution getStepExecution(long jobExecutionId, long stepExecutionId);

/**
 * @param jobExecutionId
 * @return
 */
List<StepExecution> getJobSteps(long jobExecutionId);
}