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
package javax.batch.operations.exception;

public class NoSuchJobExecutionException extends
		BatchOperationsRuntimeException {

	/**
	 * NoSuchJobException is thrown when a JobOperator operation specifies a job
	 * execution that does not exist.
	 */
	private static final long serialVersionUID = 1L;

	public NoSuchJobExecutionException(final Throwable th,
			final String localizedMessage) {
		super(th, localizedMessage);
	}
	
	public NoSuchJobExecutionException(final String localizedMessage) {
		super(localizedMessage);
	}
	
	public NoSuchJobExecutionException(final Throwable th) {
		super(th);
	}
	
	public NoSuchJobExecutionException() {
		super();
	}

}
