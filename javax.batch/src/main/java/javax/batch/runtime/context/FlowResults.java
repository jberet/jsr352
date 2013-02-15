/*
 * Copyright 2013 International Business Machines Corp.
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

import javax.batch.operations.JobOperator.BatchStatus;

/**
 * The FlowResults object identifies a flow and its batch and exit status. It is
 * used by the SplitContext.
 * 
 */
public interface FlowResults {
	/**
	 * The getFlowId method returns the value of the flow element's id
	 * attribute.
	 * 
	 * @return flow id
	 */
	public String getFlowId();

	/**
	 * The getBatchStatus method returns the batch status value of the flow.
	 * 
	 * @return batch status
	 */
	public BatchStatus getBatchStatus();

	/**
	 * The getExitStatus method returns the exit status value of the flow.
	 * 
	 * @return exit status
	 */
	public String getExitStatus();
}
