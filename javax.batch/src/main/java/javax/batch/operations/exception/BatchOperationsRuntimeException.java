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

public class BatchOperationsRuntimeException extends RuntimeException {
	/**
	 * Base exception class for batch exceptions.
	 */
	private static final long serialVersionUID = 1L;
	private java.lang.String message;
	private Throwable cause;

	public BatchOperationsRuntimeException(Throwable th, String localizedMessage) {
		setMessage(localizedMessage);
		setCause(th);
	}
	
	public BatchOperationsRuntimeException(Throwable th) {
		setCause(th);
	}
	
	public BatchOperationsRuntimeException(String localizedMessage) {
		setMessage(localizedMessage);
	}
	
	public BatchOperationsRuntimeException() {
	}

	@Override
	public java.lang.String getMessage() {
		return message;
	}

	public void setMessage(java.lang.String message) {
		this.message = message;
	}

	@Override
	public Throwable getCause() {
		return cause;
	}

	public void setCause(Throwable cause) {
		this.cause = cause;
	}
}