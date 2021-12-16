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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobExecutionJpa;
import org.jberet.repository.JobInstanceJpa;
import org.jberet.repository.JobRepository;
import org.jberet.repository.JpaRepository;
import org.jberet.repository.PartitionExecutionJpa;
import org.jberet.repository.StepExecutionJpa;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.jberet.util.BatchPersistenceUnitInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JpaJobRepositoryTest {

    final static String TEST_JOB_ID = "job";

    private static JobRepository repo;
    private static EntityManagerFactoryBuilder entityManagerFactoryBuilder;
    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;

    private synchronized <T> T wrapInTransaction(Supplier<T> supplier) {
        try {
            entityManager.getTransaction().begin();
            T result = supplier.get();
            entityManager.getTransaction().commit();
            return result;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    private synchronized void wrapInTransaction(Runnable runnable) {
        try {
            entityManager.getTransaction().begin();
            runnable.run();
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");

        CompletableFuture.runAsync(() -> {
            try {
                Server.startWebServer(ds.getConnection());
            } catch (SQLException ex) {
                Logger.getLogger(JpaJobRepositoryTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        BatchPersistenceUnitInfo batchPersistenceUnitInfo = new BatchPersistenceUnitInfo();
        batchPersistenceUnitInfo.setPersistenceUnitName(JpaJobRepositoryTest.class.getSimpleName());
        batchPersistenceUnitInfo.setClassLoader(Thread.currentThread().getContextClassLoader());
        batchPersistenceUnitInfo.setProperties(new Properties());
        batchPersistenceUnitInfo.setNonJtaDataSource(ds);
        batchPersistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
        batchPersistenceUnitInfo.setSharedCacheMode(SharedCacheMode.ALL);
        batchPersistenceUnitInfo.setExcludeUnlistedClasses(false);
        batchPersistenceUnitInfo.setManagedClassNames(
                Arrays.asList(
                        JobInstanceJpa.class,
                        JobExecutionJpa.class,
                        StepExecutionJpa.class,
                        PartitionExecutionJpa.class
                ).stream().map(
                        c -> c.getCanonicalName()
                ).collect(Collectors.toList())
        );
        entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
                batchPersistenceUnitInfo,
                Map.of(
                        HBM2DDL_AUTO, "update",
                        DIALECT, "org.hibernate.dialect.H2Dialect",
                        SHOW_SQL, "true"
                )
        );
        entityManagerFactory = entityManagerFactoryBuilder.build();
        entityManager = entityManagerFactory.createEntityManager();
        repo = new JpaRepository(entityManager);

    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (Objects.nonNull(entityManager) && entityManager.isOpen()) {
            entityManager.close();
        }
        if (Objects.nonNull(entityManagerFactory) && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        if (Objects.nonNull(entityManagerFactoryBuilder)) {
            entityManagerFactoryBuilder.cancel();
        }
    }

    @Test
    public void addRemoveJob() throws Exception {
        final Job job = ArchiveXmlLoader.loadJobXml(TEST_JOB_ID, this.getClass().getClassLoader(), new ArrayList<>(), new MetaInfBatchJobsJobXmlResolver());
        repo.removeJob(job.getId());  //the job has not been added, but removeJob should still work

        repo.addJob(new ApplicationAndJobName(null, TEST_JOB_ID), job);
        Assert.assertTrue(repo.jobExists(TEST_JOB_ID));

        repo.removeJob(TEST_JOB_ID);
        Assert.assertFalse(repo.jobExists(TEST_JOB_ID));

        repo.removeJob(job.getId());
    }

    @Test
    public void getJobExecutionsByJob() throws Exception {
        final Job job = ArchiveXmlLoader.loadJobXml(TEST_JOB_ID, this.getClass().getClassLoader(), new ArrayList<>(), new MetaInfBatchJobsJobXmlResolver());

        wrapInTransaction(() -> repo.addJob(new ApplicationAndJobName(null, TEST_JOB_ID), job));
        Assert.assertTrue(repo.jobExists(TEST_JOB_ID));

        for (int i = 0; i < 3; i++) {
            JobInstanceImpl jobInstance = wrapInTransaction(() -> repo.createJobInstance(job, null, this.getClass().getClassLoader()));
            wrapInTransaction(() -> repo.createJobExecution(jobInstance, null));
        }

        List<Long> jobExecutions = repo.getJobExecutionsByJob(TEST_JOB_ID);
        Assert.assertEquals(3, jobExecutions.size());

        jobExecutions = repo.getJobExecutionsByJob(TEST_JOB_ID, 1);
        Assert.assertEquals(1, jobExecutions.size());
    }

}
