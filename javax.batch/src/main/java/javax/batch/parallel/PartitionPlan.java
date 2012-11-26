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
package javax.batch.parallel;

/**
 *
 * PartitionPlan is a helper class that carries partition processing
 * information set by the @PartitionAlgorithm method.
 *
 * A PartitionPlan contains:
 * <ol>
 * <li>number of subjobs to dispatch for a partitioned step of flow</li>
 * <li>substitution properties for each subjob comprising the partitioned
 * step or flow</li>
 * </ol>
 *
 * @see javax.batch.annotation.parallel.PartitionAlgorithm
 */
import java.io.Externalizable;
import java.util.Properties;

public interface PartitionPlan extends Externalizable {
    
    /**
     * Set count of subjobs.
     * 
     * @param count
     *            specifies the subjob count
     */
    public void setSubJobCount(int count);

    /**
     * Sets array of substitution Properties objects for the set of subjobs.
     * 
     * @param props
     *            specifies the Properties object array
     */
    public void setSubJobProperties(Properties[] props);

    /**
     * Gets count of subjobs.
     * 
     * @return subjob count
     */
    public int getSubJobCount();

    /**
     * Gets array of subjob Properties objects for subjobs.
     * 
     * @return subjob Properties object array
     */
    public Properties[] getSubJobProperties();
}