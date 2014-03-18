/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.repository;

import java.lang.reflect.Method;
import java.util.Properties;

import org.jberet._private.BatchMessages;
import org.jberet.spi.BatchEnvironment;

public final class JobRepositoryFactory {
    public static final String JOB_REPOSITORY_TYPE_KEY = "job-repository-type";
    public static final String REPOSITORY_TYPE_IN_MEMORY = "in-memory";
    public static final String REPOSITORY_TYPE_JDBC = "jdbc";
    public static final String REPOSITORY_TYPE_MONGODB = "mongodb";

    private JobRepositoryFactory() {
    }

    public static JobRepository getJobRepository(final BatchEnvironment batchEnvironment) {
        String repositoryType = null;
        if (batchEnvironment != null) {
            final Properties configProperties = batchEnvironment.getBatchConfigurationProperties();
            repositoryType = configProperties.getProperty(JOB_REPOSITORY_TYPE_KEY);
            if (repositoryType != null) {
                repositoryType = repositoryType.trim();
            }
        }
        if (repositoryType == null || repositoryType.isEmpty() || repositoryType.equalsIgnoreCase(REPOSITORY_TYPE_JDBC)) {
            return JdbcRepository.getInstance(batchEnvironment);
        } else if (repositoryType.equalsIgnoreCase(REPOSITORY_TYPE_IN_MEMORY)) {
            return InMemoryRepository.getInstance(batchEnvironment);
        } else if (repositoryType.equalsIgnoreCase(REPOSITORY_TYPE_MONGODB)) {
            final String className = "org.jberet.repository.MongoRepository";
            try {
                final Class c = batchEnvironment.getClassLoader().loadClass(className);
                final Method getInstanceMethod = c.getMethod("getInstance", new Class[]{BatchEnvironment.class});
                final Object mongoRepository = getInstanceMethod.invoke(null, batchEnvironment);
                return (JobRepository) mongoRepository;
            } catch (final Exception e) {
                throw BatchMessages.MESSAGES.failToCreateArtifact(e, className);
            }
        } else {
            throw BatchMessages.MESSAGES.unrecognizedJobRepositoryType(repositoryType);
        }
    }
}
