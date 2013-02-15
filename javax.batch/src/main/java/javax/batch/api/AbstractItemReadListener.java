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
 * ItemReadListener intercepts item reader processing.
 * 
 * @param <T>
 *            specifies the type processed by an item reader.
 */
public abstract class AbstractItemReadListener<T> implements ItemReadListener<T> {

    @Override
    public void afterRead(T item) throws Exception {
        
    }

    @Override
    public void beforeRead() throws Exception {
        
    }

    @Override
    public void onReadError(Exception ex) throws Exception {
        
    }

}
