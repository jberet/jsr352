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
package javax.batch.operations;

public class JobExecutionNotRunningException extends
		BatchOperationsRuntimeException {
	/**
	 * JobExecutionNotRunningException is thrown when a JobOperator operation
	 * that applies to a running job execution specifies a job execution that is
	 * not running.
	 */
	private static final long serialVersionUID = 1L;
	
	public JobExecutionNotRunningException(final Throwable th,
			final String localizedMessage) {
		super(th, localizedMessage);
	}
	
	public JobExecutionNotRunningException(final String localizedMessage) {
		super(localizedMessage);
	}
	
	public JobExecutionNotRunningException(final Throwable th) {
		super(th);
	}
	
	public JobExecutionNotRunningException() {
		super();
	}

}
