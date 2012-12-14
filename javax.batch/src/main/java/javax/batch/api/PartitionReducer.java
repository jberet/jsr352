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
package javax.batch.api;

/**
 * PartitionReducer provides unit of work demarcation across partitions. It is
 * not a JTA transaction; no resources are enlisted. Rather, it provides
 * transactional flow semantics to facilitate finalizing merge or compensation
 * logic.
 * 
 */
public interface PartitionReducer {
	/**
	 * The beginPartitionedStep method receives control at the start of
	 * partition processing. It receives control before the PartitionMapper is
	 * invoked and before any partitions are started.
	 * 
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void beginPartitionedStep() throws Exception;

	/**
	 * The beforePartitionedStepCompletion method receives control at the end of
	 * a partitioned step processing. It receives control after all partitions
	 * have completed. It does not receive control if the PartitionReducer is
	 * rolling back.
	 * 
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void beforePartitionedStepCompletion() throws Exception;

	/**
	 * The rollbackPartitionedStep method that receives control if the runtime
	 * is rolling back a partition reducer. Any partition threads still running
	 * are stopped before this method is invoked. This method receives control
	 * if any of the following conditions are true:
	 * <p>
	 * <ol>
	 * <li>One or more partitions end with a Batch Status of STOPPED or FAILED.</li>
	 * <li>Any of the following partitioned step callbacks throw an exception:</li>
	 * <ol>
	 * <li>PartitionMapper</li>
	 * <li>PartitionReducer</li>
	 * <li>PartitionCollector</li>
	 * <li>PartitionAnalyzer</li>
	 * </ol>
	 * <li>A job with partitioned steps is restarted.</li> </ol>
	 * 
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void rollbackPartitionedStep() throws Exception;

	/**
	 * The afterPartitionedStepCompletion method that receives control at the
	 * end of a partition processing. It receives a status string that
	 * identifies the outcome of the partition processing. The status string
	 * value is either "COMMIT" or "ROLLBACK".
	 * 
	 * @param status
	 *            specifies the outcome of the partitioned step. Values are
	 *            "COMMIT" or "ROLLBACK".
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void afterPartitionedStepCompletion(String status) throws Exception;
}
