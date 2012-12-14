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

import java.util.List;

/**
 * RetryListener intercepts retry processing.
 * 
 * @param <T>
 *            specifies the type of item processed by a chunk type step.
 */
public interface RetryListener<T> {
	/**
	 * The onRetryReadException method that receives control when a retryable
	 * exception is thrown from an item reader. The method receives as input the
	 * exception. The method receives control in same transactional scope as the
	 * item reader. If this method throws a an exception, the job ends in the
	 * FAILED state.
	 * 
	 * @param ex
	 *            specifies the exception thrown by an item reader.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onRetryReadException(Exception ex) throws Exception;

	/**
	 * The onRetryReadItem method that receives control after a retryable
	 * exception is thrown from an item reader. This method is invoked before
	 * the item reader is called again. If the exception is a non-rollback
	 * exception, this method receives control in the same transaction scope as
	 * the item reader.
	 * 
	 * @param ex
	 *            specifies the exception thrown by an item reader.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onRetryReadItem(Exception ex) throws Exception;

	/**
	 * The onRetryProcessException method that receives control when a retryable
	 * exception is thrown from an item processor. The method receives as input
	 * the exception and the item being processed. The method receives control
	 * in same transactional scope as the item processor. If this method throws
	 * a an exception, the job ends in the FAILED state.
	 * 
	 * @param item
	 *            specifies the item passed to an item processor.
	 * @param ex
	 *            specifies the exception thrown by an item processor.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onRetryProcessException(T item, Exception ex) throws Exception;

	/**
	 * The onRetryProcessItem method that receives control after a retryable
	 * exception is thrown from an item processor. This method is invoked before
	 * the item processor is called again. If the exception is a non-rollback
	 * exception, this method receives control in the same transaction scope as
	 * the item processor .
	 * 
	 * @param item
	 *            specifies the item passed to an item processor.
	 * @param ex
	 *            specifies the exception thrown by an item processor.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onRetryProcessItem(T item, Exception ex) throws Exception;

	/**
	 * The onRetryWriteException method that receives control when a retryable
	 * exception is thrown from an item writer. The method receives as input the
	 * exception and the list of items being written. The method receives
	 * control in same transactional scope as the item writer. If this method
	 * throws a an exception, the job ends in the FAILED state.
	 * 
	 * @param items
	 *            specify the items passed to an item writer.
	 * @param ex
	 *            specifies the exception thrown by an item writer.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onRetryWriteException(List<T> items, Exception ex)
			throws Exception;

	/**
	 * The onRetryWriteItems method that receives control after a retryable
	 * exception is thrown from an item writer. This method is invoked before
	 * the item processor is called again . If the exception is a non- rollback
	 * exception, this method receives control in the same transaction scope as
	 * the item writer.
	 * 
	 * @param items
	 *            specify the items passed to an item writer.
	 * @param ex
	 *            specifies the exception thrown by an item writer.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onRetryWriteItems(List<T> items, Exception ex) throws Exception;
}
