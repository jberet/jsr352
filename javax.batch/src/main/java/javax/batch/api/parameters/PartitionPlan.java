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
package javax.batch.api.parameters;

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
import java.io.Externalizable;
import java.util.Properties;
public interface PartitionPlan extends Externalizable {
/**
* Set count of Partitions.
* @param count specifies the Partition count
*/
public void setPartitionCount(int count) ;
/**
* Set count of threads. Defaults to zero, which means * thread count is equal to partition count.
* @param count specifies the thread count
*/
public void setThreadCount(int count) ;
/**
* Sets array of substitution Properties objects for the set of Partitions.
* @param props specifies the Properties object array
*/
public void setPartitionProperties(Properties[] props) ;
/**
* Gets count of Partitions.
* @return Partition count
*/
public int getPartitionCount() ;

/**
* Gets count of threads.
* @return thread count
*/
public int getThreadCount() ;
/**
* Gets array of Partition Properties objects for Partitions.
* @return Partition Properties object array
*/
public Properties[] getPartitionProperties() ;
}