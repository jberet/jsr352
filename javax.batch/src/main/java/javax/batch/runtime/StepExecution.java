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
package javax.batch.runtime;

import java.io.Serializable;
import java.util.Date;

import javax.batch.operations.JobOperator.BatchStatus;


public interface StepExecution<P extends Serializable> {
	/**
	 * Get unique id for this StepExecution.
	 * @return StepExecution id 
	 */
	public long getExecutionId();
	/**
	 * Get step name.
	 * @return value of 'id' attribute from <step>
	 */
	public String getStepName();	
	/**
	 * Get step containment. The purpose of this 
	 * method is to make it possible to distinguish 
	 * among steps with the same names that occur within
	 * different containment. A containment is  
	 * an array of 'id' attribute names from those XML elements 
	 * that contain the step represented by this StepExecution. 
	 * The 'id' attribute values of the containment are 
	 * ordered from outer scope to inner scope, starting with
	 * the first containing element that is a direct child element 
	 * of the job element.
	 * <p>
	 * E.g.
	 * <p>
	 * <job id="job1">
	 *    <flow id="flow1">
	 *       <split id="split1">
	 *          <step id="step1">
	 *          </step>
	 *       </split>
	 *    </flow>
	 * </job>
	 * <p>
	 * The getStepName method would return the following
	 * containment array for "step1": {"flow1","split1"}. 
	 * 
	 * @return array of containment names.
	 *
	 */
	String[] getStepContainment(); 
	/**
	 * Get batch status of this step execution.
	 * @return batch status.
	 */
	public BatchStatus getBatchStatus();
	/**
	 * Get time this step started.
	 * @return date (time)
	 */
	public Date getStartTime();
	/**
	 * Get time this step ended.
	 * @return date (time)
	 */
	public Date getEndTime();
	/**
	 * Get exit status of step.
	 * @return exit status
	 */
	public String getExitStatus();
	/**
	 * Get user persistent data
	 * @return persistent data 
	 */
	public P getUserPersistentData();
	/**
	 * Get step metrics
	 * @return array of metrics 
	 */
	public Metric[] getMetrics();

}