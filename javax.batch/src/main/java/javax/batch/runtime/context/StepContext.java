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

package javax.batch.runtime.context;

/**
 *
 * StepContext is the class field type associated with the @StepContext
 * annotation. A StepContext provides information about the current step
 * of a job execution.
 *
 * @see javax.batch.annotation.context.StepContext
 */
import java.io.Externalizable;
import java.util.Properties;

import javax.batch.runtime.Metric;
import javax.batch.operations.JobOperator.BatchStatus;

public interface StepContext<T, P extends Externalizable> extends BatchContext<T> {
	/**
	 * The getId method returns the current step's id. * This is the value from
	 * the id attribute of the Job * XML <step> element.
	 * 
	 * @return step id
	 */
	public String getId();

	/**
	 * The getStepExecutionId method returns the current step's * execution id.
	 * 
	 * @return step execution id
	 */
	public long getStepExecutionId();

	/**
	 * The getProperties method returns the step level properties specified in a
	 * job definition.
	 * 
	 * @return job level properties
	 */
	public Properties getProperties();

	/**
	 * The getPersistentUserData method returns a persistent data object
	 * belonging to the current step. The user data type must implement
	 * java.util.Externalizable. This data is saved as part of a step's
	 * checkpoint. For a step that does not do checkpoints, it is saved after
	 * the step ends. It is available upon restart.
	 * 
	 * @return user-specified type
	 */
	public P getPersistentUserData();

	/**
	 * The setPersistentUserData method stores a persistent data object into the
	 * current step. The user data type must implement java.util.Externalizable.
	 * This data is saved as part of a step's checkpoint. For a step that does
	 * not do checkpoints, it is saved after the step ends. It is available upon
	 * restart.
	 * 
	 * @param data
	 *            is the user-specified type
	 */
	public void setPersistentUserData(P data);

	/**
	* The getBatchStatus method returns the current batch status of the 
	* current step. This value is set by the batch runtime and changes as 
	* the batch status changes.
	* @return batch status string
	*/
	public BatchStatus getBatchStatus();

	/**
	 * The getExitStatus method simply returns the exit status value stored into
	 * the step context through the setExitStatus method or null.
	 * 
	 * @return exit status string
	 */
	public String getExitStatus();

	/**
	 * The setExitStatus method assigns the user-specified exit status for the
	 * current step. When the step ends, the exit status of the step is the
	 * value specified through setExitStatus. If setExitStatus was not called or
	 * was called with a null value, then the exit status defaults to the batch
	 * status of the step.
	 * 
	 * @Param status string
	 */
	public void setExitStatus(String status);

	/**
	 * The getException method returns the last exception thrown from a step
	 * level batch artifact to the batch runtime.
	 * 
	 * @return the last exception
	 */
	public Exception getException();

	/**
	 * The getMetrics method returns an array of step level metrics. These are
	 * things like commits, skips, etc.
	 * 
	 * @see javax.batch.runtime.metric.Metric for definition of standard
	 *      metrics.
	 * @return metrics array
	 */
	public Metric[] getMetrics();
}