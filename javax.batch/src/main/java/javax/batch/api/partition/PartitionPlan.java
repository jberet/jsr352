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
package javax.batch.api.partition;

/**
*
* PartitionPlan is a helper class that carries partition processing
* information set by the @PartitionMapper method.
*
* A PartitionPlan contains:
* <ol>
* <li>number of Partitions step partitions </li>
* <li>number of threads on which to execute the partitions</li>
* <li>substitution properties for each Partition </li>
* </ol>
*
* @see javax.batch.annotation.parallel.PartitionMapper
*/
import java.io.Serializable;
import java.util.Properties;

public interface PartitionPlan extends Serializable {
	/**
	 * Set number of partitions. 
	 * @param count specifies the partition count
	 */
	public void setPartitions(int count);

	/**
	 * Specify whether or not to override the partition
	 * count from the previous job execution.  This applies
	 * only to step restart. The default is false.
	 * <p> 
	 * When false is specified, the
	 * partition count from the previous job execution is used
	 * and any new value set for partition count in the current run
	 * is ignored. In addition, partition results from the previous 
	 * job execution are remembered, and only incomplete partitions
	 * are reprocessed. 
	 * <p>
	 * When true is specified, the partition count from the current run
	 * is used and all results from past partitions are discarded. Any
	 * resource cleanup or back out of work done in the previous run is the 
	 * responsibility of the application. The PartitionReducer artifact's 
	 * rollbackPartitionedStep method is invoked during restart before any
	 * partitions begin processing to provide a cleanup hook.
	 */
	public void setPartitionsOverride(boolean override); 

	/**
	 * Return current value of partition override setting.
	 * @return override setting.
	 */
	public boolean getPartitionsOverride();

	/**
	 * Set number of threads. Defaults to zero, which means
	 * thread count is equal to partition count.
	 * @param count specifies the thread count
	 */
	public void setThreads(int count);

	/**
	 * Sets array of substitution Properties objects for the set of Partitions. 
	 * @param props specifies the Properties object array
	 */
	public void setPartitionProperties(Properties[] props);
		
	/**
	 * Gets count of Partitions.
	 * @return Partition count
	 */
	public int getPartitions();

	/**
	 * Gets count of threads.
	 * @return thread count
	 */
	public int getThreads();

		
	/**
	 * Gets array of Partition Properties objects for Partitions.
	 * @return Partition Properties object array
	 */
	public Properties[] getPartitionProperties();
}