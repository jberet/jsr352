package org.jberet.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.h2.jdbcx.JdbcDataSource;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.jpa.repository.entity.JobExecutionJpa;
import org.jberet.jpa.repository.entity.JobInstanceJpa;
import org.jberet.repository.JobRepository;
import org.jberet.jpa.repository.JpaRepository;
import org.jberet.jpa.repository.entity.PartitionExecutionJpa;
import org.jberet.jpa.repository.entity.StepExecutionJpa;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.jberet.jpa.util.BatchPersistenceUnitInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author a.moscatelli
 */
public class JpaJobRepositoryTest {

    final static String TEST_JOB_ID = "job";
    final static Job job = ArchiveXmlLoader.loadJobXml(TEST_JOB_ID, JpaJobRepositoryTest.class.getClassLoader(), new ArrayList<>(), new MetaInfBatchJobsJobXmlResolver());

    private static JobRepository repo;
    private static EntityManagerFactoryBuilder entityManagerFactoryBuilder;
    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;

    @BeforeClass
    public static void beforeClass() throws Exception {

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

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
        Map integration = new HashMap<>();
        integration.put(HBM2DDL_AUTO, "create");
        integration.put(SHOW_SQL, "true");
        entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
                batchPersistenceUnitInfo,
                integration
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

    @Before
    public void before() {
        entityManager.getTransaction().begin();
    }

    @After
    public void after() {
        entityManager.getTransaction().commit();
    }

    @Test
    public void addRemoveJob() throws Exception {
        repo.removeJob(job.getId());  //the job has not been added, but removeJob should still work

        repo.addJob(new ApplicationAndJobName(null, TEST_JOB_ID), job);
        Assert.assertTrue(repo.jobExists(TEST_JOB_ID));

        repo.removeJob(TEST_JOB_ID);
        Assert.assertFalse(repo.jobExists(TEST_JOB_ID));

        repo.removeJob(job.getId());
    }

    @Test
    public void getJobExecutionsByJobExtended() throws Exception {
        repo.addJob(new ApplicationAndJobName(null, TEST_JOB_ID), job);
        Assert.assertTrue(repo.jobExists(TEST_JOB_ID));

        for (int i = 0; i < 3; i++) {
            JobInstanceImpl jobInstance = repo.createJobInstance(job, null, this.getClass().getClassLoader());
            repo.createJobExecution(jobInstance, null);
        }

        List<Long> jobExecutions = repo.getJobExecutionsByJob(TEST_JOB_ID);
        Assert.assertEquals(3, jobExecutions.size());

        jobExecutions = repo.getJobExecutionsByJob(TEST_JOB_ID, 1);
        Assert.assertEquals(1, jobExecutions.size());

        JobInstanceImpl jobInstance = repo.createJobInstance(job, null, this.getClass().getClassLoader());
        JobExecutionImpl jobExecution = repo.createJobExecution(jobInstance, null);
        StepExecutionImpl stepExecutionImpl = new StepExecutionImpl("step1");
        repo.addStepExecution(jobExecution, stepExecutionImpl);
        repo.addStepExecution(jobExecution, new StepExecutionImpl("step2"));
        repo.addStepExecution(jobExecution, new StepExecutionImpl("step3"));
        repo.addStepExecution(jobExecution, new StepExecutionImpl("step4"));

        List<StepExecution> stepExecutions = repo.getStepExecutions(jobExecution.getExecutionId(), null);
        Assert.assertEquals(4, stepExecutions.size());

        for (int i = 1; i <= 2; i++) {
            PartitionExecutionImpl partitionExecutionImpl = new PartitionExecutionImpl(stepExecutionImpl);
            partitionExecutionImpl.setPartitionId(i);
            repo.addPartitionExecution(stepExecutionImpl, partitionExecutionImpl);
        }

        List<PartitionExecutionImpl> partitionExecutions = repo.getPartitionExecutions(stepExecutionImpl.getStepExecutionId(), null, true, null);
        Assert.assertEquals(2, partitionExecutions.size());
    }

}
