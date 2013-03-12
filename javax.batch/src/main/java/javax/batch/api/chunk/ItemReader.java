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
package javax.batch.api.chunk;

import java.io.Serializable;

/**
 * 
 * ItemReader defines the batch artifact that reads from a stream of item for
 * chunk processing.
 * 
 * @param <T>
 *            specifies the item type returned by this reader.
 */
public interface ItemReader<T> {
	/**
	 * The open method prepare the reader to read items.
	 * 
	 * The input parameter represents the last checkpoint for this reader in a
	 * given job instance. The checkpoint data is defined by this reader and is
	 * provided by the checkpointInfo method. The checkpoint data instructs the
	 * reader where to reposition the stream upon job restart. A checkpoint
	 * value of null means reposition from the start of stream or rely on an
	 * application managed means of determining whether to position for start or
	 * restart. The persistent area of the StepContext may be used to implement
	 * application managed stream repositioning.
	 * 
	 * @param checkpoint
	 *            specifies the last checkpoint
	 * @throws Exception
	 *             is thrown for any errors.
	 */
	public void open(Serializable checkpoint) throws Exception;

	/**
	 * The close method marks the end of use of the item stream. The reader is
	 * free to do any cleanup necessary on the stream.
	 * 
	 * @throws Exception
	 *             is thrown for any errors.
	 */
	public void close() throws Exception;

	/**
	 * The readitem method returns the next item from the stream. It returns
	 * null to indicate end of stream.
	 * 
	 * @return next item or null
	 * @throws Exception
	 *             is thrown for any errors.
	 */
	public T readItem() throws Exception;

	/**
	 * The checkpointInfo method returns the current checkpoint position for
	 * this reader. It is called before a chunk checkpoint is committed.
	 * 
	 * @return checkpoint data
	 * @throws Exception
	 *             is thrown for any errors.
	 */
	public Serializable checkpointInfo() throws Exception;

}
