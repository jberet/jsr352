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

package org.jberet.test;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.transaction.UserTransaction;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Job;
import org.jberet.repository.JobRepository;
import org.jberet.repository.JobRepositoryFactory;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JobRepositoryTest {
    private static JobRepository repo;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final BatchEnvironment batchEnvironment = new BatchEnvironment() {
            @Override
            public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public ArtifactFactory getArtifactFactory() {
                return null;
            }

            @Override
            public Future<?> submitTask(final Runnable task) {
                return null;
            }

            @Override
            public <T> Future<T> submitTask(final Runnable task, final T result) {
                return null;
            }

            @Override
            public <T> Future<T> submitTask(final Callable<T> task) {
                return null;
            }

            @Override
            public UserTransaction getUserTransaction() {
                return null;
            }

            @Override
            public Properties getBatchConfigurationProperties() {
                final Properties props = new Properties();
                //props.setProperty(JobRepositoryFactory.JOB_REPOSITORY_TYPE_KEY, JobRepositoryFactory.REPOSITORY_TYPE_JDBC);
                return props;
            }
        };
        repo = JobRepositoryFactory.getJobRepository(batchEnvironment);
    }

    @Test
    public void addRemoveJob() throws Exception {
        final Job job = ArchiveXmlLoader.loadJobXml("exception-class-filter.xml", Job.class, this.getClass().getClassLoader());
        repo.removeJob(job.getId());
        final Collection<Job> jobs = repo.getJobs();
        final int existingJobsCount = jobs.size();

        repo.addJob(job);
        Assert.assertEquals(existingJobsCount + 1, repo.getJobs().size());

        repo.removeJob(job.getId());
        Assert.assertEquals(existingJobsCount, repo.getJobs().size());

        repo.removeJob(job.getId());
        Assert.assertEquals(existingJobsCount, repo.getJobs().size());
    }

}
