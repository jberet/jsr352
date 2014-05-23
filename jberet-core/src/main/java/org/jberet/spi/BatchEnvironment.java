/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
package org.jberet.spi;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;

/**
 * Represents the environment for the batch runtime.
 *
 * @author Cheng Fang
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface BatchEnvironment {
    /**
     * Gets the class loader suitable for loading application classes and batch artifacts.
     * @return an application class loader
     */
    ClassLoader getClassLoader();

    /**
     * Gets an implementation of ArtifactFactory appropriate for the current runtime environment.
     * @return an ArtifactFactory
     */
    ArtifactFactory getArtifactFactory();

    /**
     * Submits a {@link Runnable runnable} task for execution and returns a {@link java.util.concurrent.Future future} representing that
     * task. The futures {@link java.util.concurrent.Future#get() geth} method will return {@code null} upon successful
     * completion.
     *
     * @param task the task to submit
     *
     * @return a future representing pending completion of the task
     *
     * @see java.util.concurrent.ExecutorService#submit(Runnable)
     */
    Future<?> submitTask(Runnable task);

    /**
     * Submits a {@link Runnable runnable} task for execution and returns a {@link Future future} representing that
     * task. The {@link Future future's} get method will return the given result upon successful completion.
     *
     * @param task   the task to submit
     * @param result the result to return
     * @param <T>    the type of the result
     *
     * @return a {@link Future future} representing pending completion of the task
     *
     * @see java.util.concurrent.ExecutorService#submit(Runnable, Object)
     */
    <T> Future<T> submitTask(Runnable task, T result);

    /**
     * Submits a value-returning task for execution and returns a {@link Future future} representing the pending
     * results of the task. The {@link Future future's} get method will return the task's result upon successful
     * completion.
     *
     * @param task the task to submit
     * @param <T>  the type of the result
     *
     * @return a {@link Future future} representing pending completion of the task
     *
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    <T> Future<T> submitTask(Callable<T> task);

    /**
     * Returns a transaction manager to be used for executions that require a transaction.
     *
     * @return a transaction manager for the environment
     */
    TransactionManager getTransactionManager();

    /**
     * Returns the job repository used for this environment.
     *
     * @return the job repository
     */
    JobRepository getJobRepository();

    /**
     * Gets configuration data for batch container.
     * @return a key-value map of batch configuration
     */
    Properties getBatchConfigurationProperties();
}
