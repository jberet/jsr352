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

import java.util.List;

/**
 * Base class for all batch context types.
 * 
 */
public interface BatchContext<T> {
	/**
	 * The getId method returns the context id. This is value of the id
	 * attribute from the Job XML execution element corresponding to this
	 * context type.
	 * 
	 * @return id string
	 */
	public String getId();

	/**
	 * The getTransientUserData method returns a transient data object belonging
	 * to the current Job XML execution element.
	 * 
	 * @return user-specified type
	 */
	public T getTransientUserData();

	/**
	 * The setTransientUserData method stores a transient data object into the
	 * current batch context.
	 * 
	 * @param data
	 *            is the user-specified type
	 */
	public void setTransientUserData(T data);

	/**
	 * The getBatchContexts method returns a list of BatchContexts corresponding
	 * to a compound Job XML execution element, either a split or a flow. The
	 * batch context of a compound execution element contains a list of batch
	 * contexts of the execution elements contained within that compound
	 * execution element. For example, if this batch context belongs to a split,
	 * the list of batch contexts is the flow contexts belonging to the flows in
	 * that split; if this batch context belongs to a flow, the list of batch
	 * contexts may contain a combination of split and step batch contexts. For
	 * regular execution elements (e.g. job, step) this method returns null.
	 * 
	 * @return list of BatchContexts
	 */
	public List<FlowContext<T>> getBatchContexts();
	
	String getBatchStatus();

	String getExitStatus();

	void setExitStatus(String status);
}
