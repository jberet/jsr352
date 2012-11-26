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
package javax.batch.runtime.context;

/**
 * 
 * FlowContext is a class field type associated with the @BatchContext
 * annotation. A FlowContext provides information about the current Flow
 * execution.
 * 
 * @see javax.batch.annotation.context.BatchContext
 */
public interface FlowContext {
    /**
     * The getId method returns the current flow's identity.
     * 
     * @return flow id string
     */
    public String getId();

    /**
     * The getBatchStatus method returns the current batch status of the current
     * job. This value is set by the batch runtime and changes as the batch
     * status changes.
     * 
     * @return batch status string
     */
    public String getBatchStatus();

    /**
     * The getExitStatus method simply returns the exit status value stored into
     * the flow context through the setExitStatus method or null.
     * 
     * @return exit status string
     */
    public String getExitStatus();

    /**
     * The setExitStatus method assigns the user-specified exit status for the
     * current flow. When the flow ends, the exit status of the flow is the
     * value specified through setExitStatus. If setExitStatus was not called or
     * was called with a null value, then the exit status defaults to the batch
     * status of the flow.
     * 
     * @Param status string
     */
    public void setExitStatus(String status);
}