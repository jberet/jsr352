/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.repository;

import java.util.Properties;

import org.jberet.config.BatchConfig;
import org.jberet.util.BatchLogger;

public final class JobRepositoryFactory {
    public static final String JOB_REPOSITORY_TYPE_KEY = "job-repository-type";
    public static final String REPOSITORY_TYPE_IN_MEMORY = "in-memory";
    public static final String REPOSITORY_TYPE_JDBC = "jdbc";

    private JobRepositoryFactory() {
    }

    public static JobRepository getJobRepository() {
        Properties configProperties = BatchConfig.getInstance().getConfigProperties();
        String repositoryType = configProperties.getProperty(JOB_REPOSITORY_TYPE_KEY);
        if (repositoryType == null || repositoryType.equals(REPOSITORY_TYPE_JDBC)) {
            return JdbcRepository.getInstance();
        } else if(repositoryType.equals(REPOSITORY_TYPE_IN_MEMORY)) {
            return InMemoryRepository.getInstance();
        } else {
            throw BatchLogger.LOGGER.unrecognizedJobRepositoryType(repositoryType);
        }
    }
}
