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
