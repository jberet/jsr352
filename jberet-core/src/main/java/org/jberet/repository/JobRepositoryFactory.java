/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import org.jberet._private.BatchLogger;
import org.jberet.spi.BatchEnvironment;

/**
 * @deprecated use the {@link org.jberet.spi.BatchEnvironment#getJobRepository()}
 */
@Deprecated
public final class JobRepositoryFactory {
    public static final String JOB_REPOSITORY_TYPE_KEY = "job-repository-type";
    public static final String REPOSITORY_TYPE_IN_MEMORY = "in-memory";
    public static final String REPOSITORY_TYPE_JDBC = "jdbc";
    public static final String REPOSITORY_TYPE_MONGODB = "mongodb";
    private JobRepositoryFactory() {
    }

    public static JobRepository getJobRepository(final BatchEnvironment batchEnvironment) {
        final JobRepository jobRepository = batchEnvironment.getJobRepository();
        injectTransactionManager(batchEnvironment, jobRepository);
        return jobRepository;
    }

    /**
     * Supplies a {@link org.jberet.repository.JdbcRepository} with the environment's transaction
     * manager, so that it can tell a real JTA transaction it must defer to from the no-op
     * {@code LocalTransactionManager} it must ignore.
     * <p>
     * Other repository types take no transaction manager, and an environment that cannot provide
     * one simply leaves the repository managing its own local transactions.
     *
     * @param batchEnvironment the batch environment, may be {@code null}
     * @param jobRepository    the job repository to configure, may be {@code null}
     *
     * @since 2.2.0
     */
    public static void injectTransactionManager(final BatchEnvironment batchEnvironment,
                                                final JobRepository jobRepository) {
        if (!(jobRepository instanceof JdbcRepository) || batchEnvironment == null) {
            return;
        }
        try {
            ((JdbcRepository) jobRepository).setTransactionManager(batchEnvironment.getTransactionManager());
        } catch (final RuntimeException e) {
            //an environment without a transaction manager is a supported configuration
            BatchLogger.LOGGER.tracef(e, "Failed to obtain a transaction manager from %s", batchEnvironment);
        }
    }
}
