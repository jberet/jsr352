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

import javax.batch.runtime.context.BatchContext;

/**
 * Decider determines batch exit status and to influence sequencing between
 * steps, splits, and flows in a Job XML. The decider returns a String value
 * which becomes the exit status value on which the decision chooses the next
 * transition.
 * 
 * @param <T>
 *            specifies the type of the transient data in the batch context.
 */
public interface Decider<T> {
	/**
	 * The decide method receives control during job processing to set exit
	 * status between a step, split, or flow. The return value updates the
	 * current job execution's exit status.
	 * 
	 * @param ctx
	 *            specifies the last execution element's batch context. It may
	 *            be a StepContext, FlowContext, or SplitContext.
	 * @return updated job exit status
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public String decide(BatchContext<T> ctx) throws Exception;
}
