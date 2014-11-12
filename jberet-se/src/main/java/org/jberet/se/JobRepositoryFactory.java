/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.se;

import java.util.Properties;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.InfinispanRepository;
import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;
import org.jberet.repository.MongoRepository;
import org.jberet.se._private.SEBatchLogger;

/**
 * Determines the {@link org.jberet.repository.JobRepository job repistory} to use.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JobRepositoryFactory {

    private static final JobRepositoryFactory INSTANCE = new JobRepositoryFactory();

    private JobRepository jobRepository;

    private JobRepositoryFactory() {
    }

    public static JobRepository getJobRepository(final Properties configProperties) {
        String repositoryType = null;
        if (configProperties != null) {
            repositoryType = configProperties.getProperty(BatchSEEnvironment.JOB_REPOSITORY_TYPE_KEY);
            if (repositoryType != null) {
                repositoryType = repositoryType.trim();
            }
        }
        JobRepository jobRepository;
        synchronized (INSTANCE) {
            jobRepository = INSTANCE.jobRepository;
            if (repositoryType == null || repositoryType.equalsIgnoreCase(BatchSEEnvironment.REPOSITORY_TYPE_IN_MEMORY)) {
                if (!(jobRepository instanceof InMemoryRepository)) {
                    jobRepository = INSTANCE.jobRepository = InMemoryRepository.getInstance();
                }
            } else if (repositoryType.isEmpty() || repositoryType.equalsIgnoreCase(BatchSEEnvironment.REPOSITORY_TYPE_JDBC)) {
                if (!(jobRepository instanceof JdbcRepository)) {
                    jobRepository = INSTANCE.jobRepository = JdbcRepository.create(configProperties);
                }
            } else if (repositoryType.equalsIgnoreCase(BatchSEEnvironment.REPOSITORY_TYPE_MONGODB)) {
                if (!(jobRepository instanceof MongoRepository)) {
                    jobRepository = INSTANCE.jobRepository = MongoRepository.create(configProperties);
                }
            } else if (repositoryType.equalsIgnoreCase(BatchSEEnvironment.REPOSITORY_TYPE_INFINISPAN)) {
                if (!(jobRepository instanceof InfinispanRepository)) {
                    jobRepository = INSTANCE.jobRepository = InfinispanRepository.create(configProperties);
                }
            } else {
                throw SEBatchLogger.LOGGER.unrecognizedJobRepositoryType(repositoryType);
            }
        }
        return jobRepository;
    }
}
