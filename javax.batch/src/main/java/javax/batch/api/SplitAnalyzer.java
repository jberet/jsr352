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

import java.io.Externalizable;

/**
 * SplitAnalyer receives control to analyze results from each split flow. If a
 * split collector is configured on the split, the split analyzer receives
 * control to process the results from the split collector. While a separate
 * split collector instance is invoked on each thread processing a split flow,
 * the split analyzer runs on a single, consistent thread each time it is
 * invoked. The SplitContext is in scope when the split analyzer receives
 * control. The split analyzer also receives control to analyze the final result
 * from the processing of each split flow.
 * 
 */
public interface SplitAnalyzer {
	/**
	 * The analyzeCollectorData method receives control each time a split
	 * collector sends its payload. It receives as an input the Externalizable
	 * object from the collector.
	 * 
	 * @param data
	 *            specifies the payload sent by a SplitCollector.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void analyzeCollectorData(Externalizable data) throws Exception;

	/**
	 * The analyzeStatus method receives control each time a split flow ends. It
	 * receives as input the job batch and exit status strings current when the
	 * flow ends.
	 * 
	 * @param batchStatus
	 *            specifies the batch status of a partition.
	 * @param exitStatus
	 *            specifies the exit status of a partition.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void analyzeStatus(String batchStatus, String exitStatus)
			throws Exception;
}
