/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.jberet.spi;

import java.util.Properties;
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
     * Submits a {@link Runnable runnable} task for execution.
     *
     * @param task the task to submit
     */
    void submitTask(JobTask task);

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
     * Returns the job XML resolver used to locate the job XML content.
     *
     * @return the job XML resolver
     */
    JobXmlResolver getJobXmlResolver();

    /**
     * Gets configuration data for batch container.
     * @return a key-value map of batch configuration
     */
    Properties getBatchConfigurationProperties();

    /**
     * @return application name
     */
    String getApplicationName();
}
