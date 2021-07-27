/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.test;

import java.util.ArrayList;
import java.util.Properties;
import javax.transaction.xa.XAResource;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JobRepositoryTest {

    static final Transaction NO_OP_TRANSACTION = new Transaction() {
        @Override
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
        }

        @Override
        public int getStatus() throws SystemException {
            return 0;
        }

        @Override
        public boolean enlistResource(final XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
            return false;
        }

        @Override
        public boolean delistResource(final XAResource xaRes, final int flag) throws IllegalStateException, SystemException {
            return false;
        }

        @Override
        public void registerSynchronization(final Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
        }
    };

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
            public void submitTask(final JobTask task) {
            }

            @Override
            public TransactionManager getTransactionManager() {
                return new TransactionManager() {
                    @Override
                    public void begin() throws NotSupportedException, SystemException {
                    }

                    @Override
                    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
                    }

                    @Override
                    public void rollback() throws IllegalStateException, SecurityException, SystemException {
                    }

                    @Override
                    public void setRollbackOnly() throws IllegalStateException, SystemException {
                    }

                    @Override
                    public int getStatus() throws SystemException {
                        return 0;
                    }

                    @Override
                    public Transaction getTransaction() throws SystemException {
                        return NO_OP_TRANSACTION;
                    }

                    @Override
                    public void setTransactionTimeout(final int seconds) throws SystemException {
                    }

                    @Override
                    public Transaction suspend() throws SystemException {
                        return NO_OP_TRANSACTION;
                    }

                    @Override
                    public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
                    }
                };
            }

            @Override
            public JobRepository getJobRepository() {
                return InMemoryRepository.getInstance();
            }

            @Override
            public JobXmlResolver getJobXmlResolver() {
                return new MetaInfBatchJobsJobXmlResolver();
            }

            @Override
            public Properties getBatchConfigurationProperties() {
                final Properties props = new Properties();
                //props.setProperty(JobRepositoryFactory.JOB_REPOSITORY_TYPE_KEY, JobRepositoryFactory.REPOSITORY_TYPE_JDBC);
                return props;
            }

            @Override
            public String getApplicationName() {
                return null;
            }
        };
        repo = batchEnvironment.getJobRepository();
    }

    @Test
    public void addRemoveJob() throws Exception {
        final String jobId = "exception-class-filter";
        final Job job = ArchiveXmlLoader.loadJobXml(jobId, this.getClass().getClassLoader(), new ArrayList<Job>(), new MetaInfBatchJobsJobXmlResolver());
        repo.removeJob(job.getId());  //the job has not been added, but removeJob should still work

        repo.addJob(new ApplicationAndJobName(null, jobId), job);
        Assert.assertTrue(repo.jobExists(jobId));

        repo.removeJob(jobId);
        Assert.assertFalse(repo.jobExists(jobId));

        repo.removeJob(job.getId());
    }

}
