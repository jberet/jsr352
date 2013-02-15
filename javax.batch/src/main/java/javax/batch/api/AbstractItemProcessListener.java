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

/**
 * The AbstractItemProcessListener provides default implementations of less
 * commonly implemented methods.
 * 
 * @param <T>
 *            specifies the item type read by the ItemProcessor paired with this
 *            ItemProcessListener.
 * 
 * @param <R>
 *            specifies the item type returned by the ItemProcessor paired with
 *            this ItemProcessListener.
 */
public abstract class AbstractItemProcessListener<T, R> implements ItemProcessListener<T, R> {
    /**
     * Override this method if the ItemProcessListener will do something before
     * the item is processed. The default implementation does nothing.
     * 
     * @param item
     *            specifies the item about to be processed.
     * @throws Exception
     *             (or subclass) if an error occurs.
     */
    @Override
    public void beforeProcess(T item) throws Exception {
    }

    /**
     * Override this method if the ItemProcessListener will do something after
     * the item is processed. The default implementation does nothing.
     * 
     * @param item
     *            specifies the item about to be processed.
     * @param result
     *            specifies the item to pass to the item writer.
     * @throws Exception
     *             (or subclass) if an error occurs.
     */
    @Override
    public void afterProcess(T item, R result) throws Exception {
    }

    /**
     * Override this method if the ItemProcessListener will do something when
     * the ItemProcessor processItem method throws an exception. The default
     * implementation does nothing.
     * 
     * @param item
     *            specifies the item about to be processed.
     * @param ex
     *            specifies the exception thrown by the item processor.
     * @throws Exception
     *             (or subclass) if an error occurs.
     */
    @Override
    public void onProcessError(T item, Exception ex) throws Exception {
    }
}
