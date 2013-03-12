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

/**
 * CheckpointAlgorithm provides a custom checkpoint policy for chunk steps.
 * 
 */
public interface CheckpointAlgorithm {
	/**
	 * The checkpointTimeout is invoked at the beginning of a new checkpoint
	 * interval for the purpose of establishing the checkpoint transaction
	 * timeout. This method is invoked if and only if the chunk step to which it
	 * belongs is configured for global transactions. It is invoked before the
	 * next checkpoint transaction begins. It receives as input the value of the
	 * global mode timeout property of the current chunk step. This method
	 * returns an integer value, which is the timeout value that will be used
	 * for the next checkpoint transaction. This method is useful to automate
	 * the setting of the checkpoint timeout based on factors known outside the
	 * job definition.
	 * 
	 * The following step properties are related to this method:
	 * <p>
	 * <ol>
	 * <li>javax.transaction.global.mode={true|false}</li>
	 * <li>javax.transaction.global.timeout={seconds} - default is 180 seconds</li>
	 * </ol>
	 * 
	 * @param timeout
	 *            specifies the global timeout value for the next checkpoint
	 *            interval
	 * @return the timeout interval to use for the next checkpoint interval
	 * @throws Exception
	 *             thrown for any errors.
	 */
	public int checkpointTimeout() throws Exception;

	/**
	 * The beginCheckpoint method is invoked before the next checkpoint interval
	 * begins. If the step is configured for global mode transactions, this
	 * method receives control before the transaction is started.
	 * 
	 * @throws Exception
	 *             thrown for any errors.
	 */
	public void beginCheckpoint() throws Exception;

	/**
	 * The isReadyToCheckpoint method is invoked by the batch runtime after each
	 * item read to determine if now is the time to checkpoint the current
	 * chunk.
	 * 
	 * @return boolean indicating whether or not to checkpoint now.
	 * @throws Exception
	 *             thrown for any errors.
	 */
	public boolean isReadyToCheckpoint() throws Exception;

	/**
	 * The endCheckpoint method is invoked after the current checkpoint is
	 * committed.
	 * 
	 * @throws Exception
	 *             thrown for any errors.
	 */
	public void endCheckpoint() throws Exception;
}
