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
        return batchEnvironment.getJobRepository();
    }
}
