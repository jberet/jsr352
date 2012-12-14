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
 * SkipListener intercepts skippable exception handling.
 * 
 * @param <T>
 *            specifies the item type processed by a chunk step.
 */
public interface SkipListener<T> {
	/**
	 * The onSkipReadItem method that receives control when a skippable
	 * exception is thrown from an item reader. The method receives the
	 * exception as an input.
	 * 
	 * @param ex
	 *            specifies the exception thrown by an item reader.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onSkipReadItem(Exception ex) throws Exception;

	/**
	 * The onSkipProcessItem method that receives control when a skippable
	 * exception is thrown from an item processor. The method receives the
	 * exception and the item to process as an input.
	 * 
	 * @param item
	 *            specifies the item passed to an item processor.
	 * @param ex
	 *            specifies the exception thrown by the item processor.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onSkipProcessItem(T item, Exception ex) throws Exception;

	/**
	 * The onSkipWriteItems method that receives control when a skippable
	 * exception is thrown from an item writer. The method receives the
	 * exception and the item that was skipped during write as an input.
	 * 
	 * @param items
	 *            specifies the list of item passed to the item writer.
	 * @param ex
	 *            specifies the exception thrown by the item writer.
	 * @throws Exception
	 *             is thrown if an error occurs.
	 */
	public void onSkipWriteItem(List<T> items, Exception ex) throws Exception;
}
