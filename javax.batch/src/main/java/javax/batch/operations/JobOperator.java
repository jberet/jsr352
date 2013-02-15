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
import javax.batch.operations.exception.JobExecutionAlreadyCompleteException;
import javax.batch.operations.exception.JobExecutionNotMostRecentException;
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
	* BatchStatus enum defines the batch status values
	* possible for a job.
	*/
	public enum BatchStatus {STARTING, STARTED, STOPPING, 
	STOPPED, FAILED, COMPLETED, ABANDONED }

	/**
	 * Returns a set of all job names known to the batch runtime.
	 * 
	 * @return a set of job names.
	 */
	Set<String> getJobNames();

	/**
	 * Returns number of instances of a job with a particular name.
	 * 
	 * @param jobName
	 *            specifies the name of the job.
	 * @return count of instances of the named job.
	 * @throws NoSuchJobException
	 */
	int getJobInstanceCount(String jobName) throws NoSuchJobException;

	/**
	 * Returns all JobInstances belonging to a job with a particular name.
	 * 
	 * @param jobName
	 *            specifies the job name.
	 * @param start
	 *            specifies the relative starting number to return from the
	 *            maximal list of job instances.
	 * @param count
	 *            specifies the number of job instances to return from the
	 *            starting position of the maximal list of job instances.
	 * @return list of JobInstances. 
	 * @throws NoSuchJobException
	 */
	List<JobInstance> getJobInstances(String jobName, int start, int count)
			throws NoSuchJobException;

	/**
	 * Returns JobInstances for all running jobs across all instances of a job
	 * with a particular name.
	 * 
	 * @param jobName
	 *            specifies the job name.
	 * @return a list of JobInstances. 
	 * @throws NoSuchJobException
	 */
	List<JobInstance> getRunningInstances(String jobName) throws NoSuchJobException;

	/**
	 * Returns all JobExecutions belonging to a particular job instance.
	 * 
	 * @param instanceId
	 *            specifies the job instance.
	 * @return List of JobExecutions. 
	 * @throws NoSuchJobInstanceException
	 */
	List<JobExecution> getExecutions(JobInstance instance) throws NoSuchJobInstanceException;

	/**
	 * Returns job parameters for a specified job instance. These are the key/value
	 * pairs specified when the instance was originally created by the start method.
	 * 
	 * @param instance
	 *            specifies the job instance.
	 * @return a Properties object containing the key/value job parameter pairs.
	 * @throws NoSuchJobInstanceException
	 */
	Properties getParameters(JobInstance instance)
			throws NoSuchJobInstanceException;

	/**
	 * Creates a new job instance and starts the first execution of that
	 * instance.
	 * 
	 * Note the Job XML describing the job is first searched for by name
	 * according to a means prescribed by the batch runtime implementation.
	 * This may vary by implementation. If the Job XML is not found by that
	 * means, then the batch runtime must search for the specified Job XML 
	 * as a resource from the META-INF/batch-jobs directory based on the 
	 * current class loader. Job XML files under META-INF/batch-jobs 
	 * directory follow a naming convention of "name".xml where "name" is
	 * the value of the jobXMLName parameter (see below).   
	 * 
	 * @param jobXMLName
	 *            specifies the name of the Job XML describing the job.
	 * @param jobParameters
	 *            specifies the keyword/value pairs for attribute 
	 *            substitution in the Job XML.
	 * @return executionId of the new job instance.
	 * @throws JobStartException
	 */
	long start(String jobXMLName, Properties jobParameters) throws JobStartException;

	/**
	 * Restarts a failed or stopped job instance.
	 * 
	 * @param executionId
	 *            specifies the execution to to restart. This execution 
	 *            must be the most recent execution that ran.
	 * @return new executionId
	 * @throws JobExecutionAlreadyCompleteException
	 * @throws NoSuchJobExecutionException
	 * @throws JobExecutionNotMostRecentException,
	 * @throws JobRestartException
	 */
	long restart(long executionId) /* exception if restart older execution */ 
			throws JobExecutionAlreadyCompleteException,
			NoSuchJobExecutionException, 
			JobExecutionNotMostRecentException, 
			JobRestartException;
	
	/**
	 * Restarts a failed or stopped job instance.
	 * 
	 * @param executionId
	 *            specifies the execution to to restart. This execution 
	 *            must be the most recent execution that ran.
	 * @return new executionId
	 * @throws JobExecutionAlreadyCompleteException
	 * @throws NoSuchJobExecutionException
	 * @throws JobExecutionNotMostRecentException,
	 * @throws JobRestartException
	 */
	long restart(long executionId, Properties overrideJobParameters) /* exception if restart older execution */ 
			throws JobExecutionAlreadyCompleteException,
			NoSuchJobExecutionException, 
			JobExecutionNotMostRecentException, 
			JobRestartException;

	/**
	 * Request a running job execution stops. This
	 * method notifies the job execution to stop 
	 * and then returns. The job execution normally 
	 * stops and does so asynchronously. Note 
	 * JobOperator cannot guarantee the jobs stops: 
	 * it is possible a badly behaved batch application 
	 * does not relinquish control.   
	 * 
	 * @param executionId
	 *            specifies the job execution to stop. 
	 *            The job execution must be running.
	 * @throws NoSuchJobExecutionException
	 * @throws JobExecutionNotRunningException
	 */
	void stop(long executionId) throws NoSuchJobExecutionException,
			JobExecutionNotRunningException;

	/**
	 * Set batch status to ABANDONED.  The instance must have 
	 * no running execution. 
	 * 
	 * @param instanceId
	 *            specifies the job instance to abandon.
	 * @throws NoSuchJobInstanceException
	 * @throws JobExecutionIsRunningException
	 */
	void abandon(JobExecution jobExecution) throws NoSuchJobInstanceException, 
			JobExecutionIsRunningException;
	
	
	/**
	 * Return the job instance for the specified execution id.
	 * 
	 * @param executionId
	 *            specifies the job execution.
	 * @return job instance
	 * @throws NoSuchJobExecutionException
	 */
	JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException;

	/**
	 * Return all job executions belonging to the specified job instance.
	 * 
	 * @param jobInstance
	 *            specifies the job instance.
	 * @return list of job executions
	 * @throws NoSuchJobInstanceException 
	 */
	List<JobExecution> getJobExecutions(JobInstance instance) throws NoSuchJobInstanceException;

	/**
	 * Return job execution for specified execution id
	 * 
	 * @param executionId
	 *            specifies the job execution.
	 * @return job execution
	 * @throws NoSuchJobExecutionException
	 */
	JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException;

	/**
	* Return StepExecutions for specified execution id.
	*
	* @param executionId
	* specifies the job execution.
	* @return a list of step executions (no particular ordering implied)
	* @throws NoSuchJobExecutionException
	*/
	List<StepExecution> getStepExecutions(long jobExecutionId) throws NoSuchJobExecutionException;
}