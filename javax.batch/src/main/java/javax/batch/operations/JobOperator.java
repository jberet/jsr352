/*
 * Copyright 2012 International Business Machines Corp. 
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import javax.batch.operations.exception.*;
import javax.batch.state.*;

public interface JobOperator {

	/**
	 * Returns all executionIds belonging to a particular job instance.
	 * @param instanceId identifies the job instance
	 * @return List of executionIds
	 * @throws NoSuchJobInstanceException
	 */
	List<Long> getExecutions(long instanceId) throws 
		NoSuchJobInstanceException;

	/**
	 * Returns number of instances of a job with a particular name. 
	 * @param jobName specifies the name of the job.
	 * @return count of instances of the named job. 
	 * @throws NoSuchJobException
	 */
	int getJobInstanceCount(String jobName) throws NoSuchJobException;
	
	/**
	 * Returns all instanceIds belonging to a job with a particular name.
	 * @param jobName identifies the job name.
	 * @param start identifies the relative starting number to return from 
	 * the maximal list of job instances.  
	 * @param count identifies the number of instance ids to return from 
	 * the starting position of the maximal list of job instances. 
	 * @return list of instance ids 
	 * @throws NoSuchJobException
	 */
	List<Long> getJobInstances(String jobName, int start, int count) throws 
		NoSuchJobException;

	/**
	 * Returns executionIds for all running executions across all instances 
	 * of a job with a particular name.  
	 * @param jobName identifies the job name.
	 * @return a Set of executionIds 
	 * @throws NoSuchJobException
	 */
	Set<Long> getRunningExecutions(String jobName) throws 
		NoSuchJobException;

	/**
	 * Returns job parameters for specified execution.  These are the 
	 * key/value pairs specified when the instance was started or 
	 * restarted. 
	 * @param executionId identifies the execution.
	 * @return a Properties object containing the key/value job parameter 
	 * pairs. 
	 * @throws NoSuchJobExecutionException
	 */
	Properties getParameters(long executionId) throws 
		NoSuchJobExecutionException;

	/**
	 * Creates a new job instance and starts the first execution of 
	 * that instance.
	 * @param job specifies the Job XML describing the job.
	 * @param jobParameters specifies the keyword/value pairs for 
	 * property override and substitution in Job XML. 
	 * @return initial executionId of the new job instance. 
	 * @throws JobStartException  
	 */
	// the return of a String is temp - it should return the executionID which is a Long
	Long start(String job, Properties jobParameters) throws 
		JobStartException;

	/**
	 * Restarts a failed or stopped job instance. 
	 * @param executionId belonging to the instance to restart.
	 * @param jobParameters specify replacement job parameters for the 
	 * job restart.
	 * The replacement add to and/or override the original job
	 * parameters that were specified when the instance was 
	 * originally started. 
	 * @return new executionId 
	 * @throws JobInstanceAlreadyCompleteException
	 * @throws NoSuchJobExecutionException
	 * @throws NoSuchJobException
	 * @throws JobRestartException
	 */
	Long restart(long executionId, Properties jobParameters) throws 
		JobInstanceAlreadyCompleteException, 
		NoSuchJobExecutionException, NoSuchJobException, 
		JobRestartException;
	
	/**
	 * Request a running execution stops.
	 * @param executionId specifies the execution to stop.
	 * @throws NoSuchJobExecutionException
	 * @throws JobExecutionNotRunningException
	 */
	void stop(long executionId)throws NoSuchJobExecutionException, 
		JobExecutionNotRunningException;

	/**
	 * Returns a set of all job names known to the batch runtime.
	 * @return a set of job names.
	 */
	Set<String> getJobNames();
	
	/**
	 * Return all job executions belonging to the specified job instance 
	 * @param jobInstance specifies the job instance 
	 * @return list of job executions 
	 */
	List<JobExecution> getJobExecutions(long instanceId);
	
	/**
	 * Return job execution for specified execution id
	 * @param executionId specifies the requested job execution 
	 * @return job execution 
	 */
	JobExecution getJobExecution(long executionId);
	
	/**
	 * Registers a job end callback. A job end callback is invoked by 
	 * the batch runtime when a job ends.  
	 * @param callback specifies the callback instance. 
	 * @return a callBack id.
	 * @throws CallbackRegistrationException
	 */
	long registerJobEndCallback(JobEndCallback callback) throws 
		CallbackRegistrationException;

}
