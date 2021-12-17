package org.jberet.jpa.repository;

import org.jberet.jpa.repository.entity.StepExecutionJpa;
import org.jberet.jpa.repository.entity.JobExecutionJpa;
import org.jberet.jpa.repository.entity.JobInstanceJpa;
import org.jberet.jpa.repository.entity.PartitionExecutionJpa;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Subquery;
import org.jberet.job.model.Job;
import static org.jberet.repository.TableColumns.formatException;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Optional;
import org.jberet.jpa.repository.entity.JobExecutionJpa_;
import org.jberet.jpa.repository.entity.JobInstanceJpa_;
import org.jberet.jpa.repository.entity.PartitionExecutionJpa_;
import org.jberet.jpa.repository.entity.StepExecutionJpa_;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobExecutionSelector;
import org.jberet.repository.JobRepository;
import org.jberet.jpa.util.BatchUtilJpa;

public final class JpaRepository implements JobRepository {

    final ConcurrentMap<ApplicationAndJobName, SoftReference<ExtendedJob>> jobs = new ConcurrentHashMap<>();

    final ReferenceQueue<ExtendedJob> jobReferenceQueue = new ReferenceQueue<>();

    private final EntityManager entityManager;

    public JpaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void addJob(final ApplicationAndJobName applicationAndJobName, final Job job) {
        ExtendedJob extendedJob = new ExtendedJob();
        extendedJob.setJob(job);
        extendedJob.setApplicationAndJobName(applicationAndJobName);
        for (Reference<? extends ExtendedJob> x; Objects.nonNull(x = jobReferenceQueue.poll()) && Objects.nonNull(x.get());) {
            jobs.remove(x.get().getApplicationAndJobName());
        }
        jobs.put(applicationAndJobName, new SoftReference<>(extendedJob, jobReferenceQueue));
    }

    @Override
    public Job getJob(final ApplicationAndJobName applicationAndJobName) {
        final SoftReference<ExtendedJob> jobSoftReference = jobs.get(applicationAndJobName);
        return Objects.nonNull(jobSoftReference) && Objects.nonNull(jobSoftReference.get()) ? jobSoftReference.get().getJob() : null;
    }

    @Override
    public boolean jobExists(final String jobName) {
        return jobs.keySet().stream().anyMatch(key -> Objects.equals(key.jobName, jobName));
    }

    @Override
    public Set<String> getJobNames() {
        return jobs.keySet().stream().map(key -> key.appName).collect(Collectors.toSet());
    }

    @Override
    public void removeJob(final String jobId) {
        jobs.keySet().stream().filter(
                key -> Objects.equals(key.jobName, jobId)
        ).forEach(
                key -> this.jobs.remove(key)
        );
    }

    @Override
    public JobInstanceImpl createJobInstance(Job job, String applicationName, ClassLoader classLoader) {
        JobInstanceImpl jobInstance = new JobInstanceImpl(job, applicationName, job.getId());
        JobInstanceJpa jobInstanceJpa = new JobInstanceJpa();
        jobInstanceJpa.setJobName(jobInstance.getJobName());
        jobInstanceJpa.setApplicationName(jobInstance.getApplicationName());
        this.entityManager.persist(jobInstanceJpa);
        jobInstance.setId(jobInstanceJpa.getId());
        return jobInstance;
    }

    @Override
    public void removeJobInstance(long jobInstanceId) {
        this.entityManager.remove(
                this.entityManager.find(JobInstanceJpa.class, jobInstanceId)
        );
    }

    @Override
    public JobInstance getJobInstance(long jobInstanceId) {
        JobInstanceJpa jobInstanceJpa = this.entityManager.find(JobInstanceJpa.class, jobInstanceId);
        JobInstanceImpl jobInstance = new JobInstanceImpl(
                Optional.ofNullable(
                        jobs.get(new ApplicationAndJobName(jobInstanceJpa.getApplicationName(), jobInstanceJpa.getJobName()))
                ).flatMap(
                        softReference -> Optional.ofNullable(softReference.get())
                ).map(
                        extendedJob -> extendedJob.getJob()
                ).orElse(null),
                jobInstanceJpa.getApplicationName(),
                jobInstanceJpa.getJobName()
        );
        jobInstance.setId(jobInstanceJpa.getId());
        return jobInstance;
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<JobInstanceJpa> criteriaQuery = criteriaBuilder.createQuery(JobInstanceJpa.class);
        Root<JobInstanceJpa> root = criteriaQuery.from(JobInstanceJpa.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get(JobInstanceJpa_.jobName), jobName));
        return this.entityManager.createQuery(criteriaQuery).getResultList().stream().map(
                jobInstanceJpa -> {
                    JobInstanceImpl jobInstance = new JobInstanceImpl(
                            Optional.ofNullable(
                                    jobs.get(new ApplicationAndJobName(jobInstanceJpa.getApplicationName(), jobInstanceJpa.getJobName()))
                            ).flatMap(
                                    softReference -> Optional.ofNullable(softReference.get())
                            ).map(
                                    extendedJob -> extendedJob.getJob()
                            ).orElse(null),
                            jobInstanceJpa.getApplicationName(),
                            jobInstanceJpa.getJobName()
                    );
                    jobInstance.setId(jobInstanceJpa.getId());
                    return jobInstance;
                }
        ).collect(Collectors.toList());
    }

    @Override
    public int getJobInstanceCount(String jobName) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<JobInstanceJpa> root = criteriaQuery.from(JobInstanceJpa.class);
        criteriaQuery.select(criteriaBuilder.count(root));
        criteriaQuery.where(criteriaBuilder.equal(root.get(JobInstanceJpa_.jobName), jobName));
        return this.entityManager.createQuery(criteriaQuery).getSingleResult().intValue();
    }

    @Override
    public JobExecutionImpl createJobExecution(JobInstanceImpl jobInstance, Properties jobParameters) {
        JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        JobExecutionJpa jobExecutionJpa = new JobExecutionJpa();
        jobExecutionJpa.setJobInstance(new JobInstanceJpa());
        jobExecutionJpa.getJobInstance().setId(jobInstance.getInstanceId());
        jobExecutionJpa.setBatchStatus(jobExecution.getBatchStatus());
        jobExecutionJpa.setCreateTime(jobExecution.getCreateTime());
        jobExecutionJpa.setJobParameters(jobExecution.getJobParameters());
        this.entityManager.persist(jobExecutionJpa);
        jobExecution.setId(jobExecutionJpa.getId());
        return jobExecution;
    }

    @Override
    public JobExecution getJobExecution(long jobExecutionId) {
        JobExecutionJpa jobExecutionJpa = this.entityManager.find(JobExecutionJpa.class, jobExecutionId);
        return BatchUtilJpa.from(jobExecutionJpa);
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance jobInstance) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<JobExecutionJpa> criteriaQuery = criteriaBuilder.createQuery(JobExecutionJpa.class);
        Root<JobExecutionJpa> root = criteriaQuery.from(JobExecutionJpa.class);
        criteriaQuery.select(root);
        if (Objects.nonNull(jobInstance)) {
            criteriaQuery.where(criteriaBuilder.equal(root.get(JobExecutionJpa_.jobInstance).get(JobInstanceJpa_.id), jobInstance));
        }
        return this.entityManager.createQuery(criteriaQuery).getResultList().stream().map(jobExecutionJpa -> BatchUtilJpa.from(jobExecutionJpa)
        ).collect(Collectors.toList());
    }

    private List<Long> getJobExecutions(String jobName, boolean runningExecutionsOnly, Integer limit) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<JobExecutionJpa> root = criteriaQuery.from(JobExecutionJpa.class);
        Join<JobExecutionJpa, JobInstanceJpa> join = root.join(JobExecutionJpa_.jobInstance);
        criteriaQuery.select(root.get(JobExecutionJpa_.id));
        criteriaQuery.where(
                runningExecutionsOnly
                        ? criteriaBuilder.and(
                                root.get(JobExecutionJpa_.batchStatus).in(Arrays.asList(BatchStatus.STARTED, BatchStatus.STARTING)),
                                criteriaBuilder.equal(join.get(JobInstanceJpa_.jobName), jobName)
                        )
                        : criteriaBuilder.equal(join.get(JobInstanceJpa_.jobName), jobName)
        );
        TypedQuery<Long> typedQuery = this.entityManager.createQuery(criteriaQuery);
        if (Objects.nonNull(limit)) {
            typedQuery.setMaxResults(limit);
        }
        return typedQuery.getResultList();
    }

    @Override
    public List<Long> getJobExecutionsByJob(String jobName) {
        return getJobExecutions(jobName, false, null);
    }

    @Override
    public List<Long> getJobExecutionsByJob(String jobName, Integer limit) {
        return getJobExecutions(jobName, false, limit);
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) {
        return getJobExecutions(jobName, true, null);
    }

    @Override
    public void updateJobExecution(JobExecutionImpl jobExecution, boolean fullUpdate, boolean saveJobParameters) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobExecutionJpa> createCriteriaUpdate = criteriaBuilder.createCriteriaUpdate(JobExecutionJpa.class);
        Root<JobExecutionJpa> root = createCriteriaUpdate.from(JobExecutionJpa.class);
        createCriteriaUpdate.set(root.get(JobExecutionJpa_.batchStatus), jobExecution.getBatchStatus());
        createCriteriaUpdate.set(root.get(JobExecutionJpa_.lastUpdatedTime), jobExecution.getLastUpdatedTime());
        if (fullUpdate) {
            createCriteriaUpdate.set(root.get(JobExecutionJpa_.endTime), jobExecution.getEndTime());
            createCriteriaUpdate.set(root.get(JobExecutionJpa_.exitStatus), jobExecution.getExitStatus());
            createCriteriaUpdate.set(root.get(JobExecutionJpa_.restartPosition), jobExecution.combineRestartPositionAndUser());
            if (saveJobParameters) {
                // https://hibernate.atlassian.net/browse/HBX-1870
                createCriteriaUpdate.set(root.get("jobParameters"), jobExecution.getJobParameters());
            }
        } else {
            createCriteriaUpdate.set(root.get(JobExecutionJpa_.startTime), jobExecution.getStartTime());
        }
        createCriteriaUpdate.where(
                criteriaBuilder.equal(root.get(JobExecutionJpa_.id), jobExecution.getExecutionId())
        );
        this.entityManager.createQuery(createCriteriaUpdate).executeUpdate();
    }

    private void stopPartitionExecution(JobExecutionImpl jobExecution) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<PartitionExecutionJpa> createCriteriaUpdate = criteriaBuilder.createCriteriaUpdate(PartitionExecutionJpa.class);
        Root<PartitionExecutionJpa> root = createCriteriaUpdate.from(PartitionExecutionJpa.class);
        Subquery<StepExecutionJpa> subquery = createCriteriaUpdate.subquery(StepExecutionJpa.class);
        Root<StepExecutionJpa> from = subquery.from(StepExecutionJpa.class);
        subquery.select(from);
        subquery.where(criteriaBuilder.equal(from.get(StepExecutionJpa_.jobExecution).get(JobExecutionJpa_.id), jobExecution.getExecutionId()));
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.batchStatus), BatchStatus.STOPPING);
        createCriteriaUpdate.where(
                root.get(PartitionExecutionJpa_.stepExecution).in(subquery.getSelection()),
                criteriaBuilder.equal(root.get(PartitionExecutionJpa_.batchStatus), BatchStatus.STARTED)
        );
        this.entityManager.createQuery(createCriteriaUpdate).executeUpdate();
    }

    private void stopStepExecution(JobExecutionImpl jobExecution) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<StepExecutionJpa> createCriteriaUpdate = criteriaBuilder.createCriteriaUpdate(StepExecutionJpa.class);
        Root<StepExecutionJpa> root = createCriteriaUpdate.from(StepExecutionJpa.class);
        createCriteriaUpdate.set(root.get(StepExecutionJpa_.batchStatus), BatchStatus.STOPPING);
        createCriteriaUpdate.where(
                criteriaBuilder.equal(root.get(StepExecutionJpa_.jobExecution).get(JobExecutionJpa_.id), jobExecution.getExecutionId()),
                criteriaBuilder.equal(root.get(StepExecutionJpa_.batchStatus), BatchStatus.STARTED)
        );
        this.entityManager.createQuery(createCriteriaUpdate).executeUpdate();
        this.stopPartitionExecution(jobExecution);
    }

    @Override
    public void stopJobExecution(JobExecutionImpl jobExecution) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobExecutionJpa> createCriteriaUpdate = criteriaBuilder.createCriteriaUpdate(JobExecutionJpa.class);
        Root<JobExecutionJpa> root = createCriteriaUpdate.from(JobExecutionJpa.class);
        createCriteriaUpdate.set(root.get(JobExecutionJpa_.batchStatus), BatchStatus.STOPPING);
        createCriteriaUpdate.set(root.get(JobExecutionJpa_.lastUpdatedTime), jobExecution.getLastUpdatedTime());
        createCriteriaUpdate.where(
                criteriaBuilder.equal(root.get(JobExecutionJpa_.id), jobExecution.getExecutionId()),
                criteriaBuilder.equal(root.get(JobExecutionJpa_.batchStatus), BatchStatus.STARTED)
        );
        this.entityManager.createQuery(createCriteriaUpdate).executeUpdate();
        stopStepExecution(jobExecution);
    }

    @Override
    public void removeJobExecutions(JobExecutionSelector jobExecutionSelector) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<JobExecutionJpa> criteriaQuery = criteriaBuilder.createQuery(JobExecutionJpa.class);
        Root<JobExecutionJpa> root = criteriaQuery.from(JobExecutionJpa.class);
        criteriaQuery.select(root);
        Set<Long> jobExecutionIds = this.entityManager.createQuery(criteriaQuery).getResultList().stream().map(JobExecutionJpa::getId).collect(Collectors.toSet());
        this.entityManager.createQuery(criteriaQuery).getResultList().stream().filter(
                jobExecution -> Objects.isNull(jobExecutionSelector) || jobExecutionSelector.select(jobExecution, jobExecutionIds)
        ).forEach(
                jobExecution -> this.entityManager.remove(jobExecution)
        );
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId, ClassLoader classLoader) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<StepExecutionJpa> criteriaQuery = criteriaBuilder.createQuery(StepExecutionJpa.class);
        Root<StepExecutionJpa> root = criteriaQuery.from(StepExecutionJpa.class);
        criteriaQuery.select(root);
        criteriaQuery.where(
                criteriaBuilder.equal(root.get(StepExecutionJpa_.jobExecution).get(JobExecutionJpa_.id), jobExecutionId)
        );
        return this.entityManager.createQuery(criteriaQuery).getResultList().stream().map(stepExecutionJpa -> BatchUtilJpa.from(stepExecutionJpa)
        ).collect(Collectors.toList());
    }

    @Override
    public StepExecutionImpl createStepExecution(String stepName) {
        return new StepExecutionImpl(stepName);
    }

    @Override
    public void addStepExecution(JobExecutionImpl jobExecution, StepExecutionImpl stepExecution) {
        jobExecution.addStepExecution(stepExecution);
        StepExecutionJpa stepExecutionJpa = new StepExecutionJpa();
        stepExecutionJpa.setStepName(stepExecution.getStepName());
        stepExecutionJpa.setBatchStatus(stepExecution.getBatchStatus());
        stepExecutionJpa.setStartTime(stepExecution.getStartTime());
        stepExecutionJpa.setJobExecution(new JobExecutionJpa());
        stepExecutionJpa.getJobExecution().setId(jobExecution.getExecutionId());
        this.entityManager.persist(stepExecutionJpa);
        stepExecution.setId(stepExecutionJpa.getId());

    }

    private int updateStepExecution(StepExecution stepExecution, boolean notStopping) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<StepExecutionJpa> createCriteriaUpdate = criteriaBuilder.createCriteriaUpdate(StepExecutionJpa.class);
        Root<StepExecutionJpa> root = createCriteriaUpdate.from(StepExecutionJpa.class);
        Path<Long> versionPath = root.get(StepExecutionJpa_.version);
        createCriteriaUpdate.set(versionPath, criteriaBuilder.sum(versionPath, 1L));
        createCriteriaUpdate.set(root.get(StepExecutionJpa_.endTime), stepExecution.getEndTime());
        createCriteriaUpdate.set(root.get(StepExecutionJpa_.batchStatus), stepExecution.getBatchStatus());
        createCriteriaUpdate.set(root.get(StepExecutionJpa_.exitStatus), stepExecution.getExitStatus());
        if (stepExecution instanceof AbstractStepExecution) {
            AbstractStepExecution abstractStepExecution = AbstractStepExecution.class.cast(stepExecution);
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.executionException), formatException(abstractStepExecution.getException()));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.persistenUserData), abstractStepExecution.getPersistentUserDataSerialized());
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.readCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.READ_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.writeCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.WRITE_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.commitCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.COMMIT_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.rollbackCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.ROLLBACK_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.readSkipCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.READ_SKIP_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.processSkipCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.PROCESS_SKIP_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.filterCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.FILTER_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.writeSkipCount), abstractStepExecution.getStepMetrics().get(Metric.MetricType.WRITE_SKIP_COUNT));
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.readerCheckPointInfo), abstractStepExecution.getReaderCheckpointInfoSerialized());
            createCriteriaUpdate.set(root.get(StepExecutionJpa_.writerCheckPointInfo), abstractStepExecution.getWriterCheckpointInfoSerialized());
        }
        createCriteriaUpdate.where(
                notStopping
                        ? criteriaBuilder.and(
                                criteriaBuilder.equal(root.get(StepExecutionJpa_.id), stepExecution.getStepExecutionId()),
                                criteriaBuilder.notEqual(root.get(StepExecutionJpa_.batchStatus), BatchStatus.STOPPED)
                        )
                        : criteriaBuilder.equal(root.get(StepExecutionJpa_.id), stepExecution.getStepExecutionId())
        );
        return this.entityManager.createQuery(createCriteriaUpdate).executeUpdate();
    }

    @Override
    public void updateStepExecution(StepExecution stepExecution) {
        updateStepExecution(stepExecution, false);
    }

    @Override
    public void addPartitionExecution(StepExecutionImpl enclosingStepExecution, PartitionExecutionImpl partitionExecution) {
        enclosingStepExecution.getPartitionExecutions().add(partitionExecution);
        PartitionExecutionJpa partitionExecutionJpa = new PartitionExecutionJpa();
        partitionExecutionJpa.setId(Long.valueOf(partitionExecution.getPartitionId()));
        partitionExecutionJpa.setStepExecution(new StepExecutionJpa());
        partitionExecutionJpa.getStepExecution().setId(partitionExecution.getStepExecutionId());
        partitionExecutionJpa.setBatchStatus(partitionExecution.getBatchStatus());
        this.entityManager.persist(partitionExecutionJpa);
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(String stepName, JobExecutionImpl jobExecutionToRestart, ClassLoader classLoader) {
        return jobExecutionToRestart.getStepExecutions().stream().filter(
                stepExecution -> Objects.equals(stepName, stepExecution.getStepName()) && stepExecution instanceof StepExecutionImpl
        ).map(
                stepExecution -> StepExecutionImpl.class.cast(stepExecution)
        ).findFirst().orElseGet(() -> {
                    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
                    CriteriaQuery<StepExecutionJpa> criteriaQuery = criteriaBuilder.createQuery(StepExecutionJpa.class);
                    Root<StepExecutionJpa> root = criteriaQuery.from(StepExecutionJpa.class);
                    criteriaQuery.select(root);
                    criteriaQuery.where(
                            criteriaBuilder.equal(root.get(StepExecutionJpa_.jobExecution).get(JobExecutionJpa_.jobInstance).get(JobInstanceJpa_.id), jobExecutionToRestart.getJobInstance().getInstanceId()),
                            criteriaBuilder.equal(root.get(StepExecutionJpa_.stepName), stepName)
                    );
                    criteriaQuery.orderBy(
                            criteriaBuilder.desc(root.get(StepExecutionJpa_.id))
                    );
                    TypedQuery<StepExecutionJpa> createQuery = this.entityManager.createQuery(criteriaQuery);
                    createQuery.setMaxResults(1);
                    return BatchUtilJpa.from(createQuery.getSingleResult());
                }
        );
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(long stepExecutionId, StepExecutionImpl stepExecution, boolean notCompletedOnly, ClassLoader classLoader) {
        return Optional.ofNullable(stepExecution).map(
                value -> Optional.ofNullable(value.getPartitionExecutions()).orElse(Collections.emptyList())
        ).map(
                partitionExecutions
                -> partitionExecutions.isEmpty() || !notCompletedOnly
                ? partitionExecutions
                : partitionExecutions.stream().filter(
                        sei -> !BatchStatus.COMPLETED.equals(sei.getBatchStatus())
                ).collect(Collectors.toList())
        ).filter(
                result -> !result.isEmpty()
        ).orElseGet(() -> {
                    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
                    CriteriaQuery<PartitionExecutionJpa> criteriaQuery = criteriaBuilder.createQuery(PartitionExecutionJpa.class);
                    Root<PartitionExecutionJpa> root = criteriaQuery.from(PartitionExecutionJpa.class);
                    criteriaQuery.select(root);
                    criteriaQuery.where(
                            criteriaBuilder.equal(root.get(PartitionExecutionJpa_.stepExecution).get(StepExecutionJpa_.id), stepExecutionId)
                    );
                    criteriaQuery.orderBy(
                            criteriaBuilder.asc(root.get(PartitionExecutionJpa_.id))
                    );
                    return this.entityManager.createQuery(criteriaQuery).getResultList().stream().filter(
                            partitionExecutionJpa -> !notCompletedOnly || !BatchStatus.COMPLETED.equals(partitionExecutionJpa.getBatchStatus())
                    ).map(partitionExecutionJpa -> BatchUtilJpa.from(partitionExecutionJpa)
                    ).collect(Collectors.toList());
                }
        );
    }

    @Override
    public int countStepStartTimes(String stepName, long jobInstanceId) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<StepExecutionJpa> root = criteriaQuery.from(StepExecutionJpa.class);
        criteriaQuery.select(criteriaBuilder.countDistinct(root.get(StepExecutionJpa_.id)));
        criteriaQuery.where(
                criteriaBuilder.equal(root.get(StepExecutionJpa_.jobExecution).get(JobExecutionJpa_.jobInstance).get(JobInstanceJpa_.id), jobInstanceId),
                criteriaBuilder.equal(root.get(StepExecutionJpa_.stepName), stepName)
        );
        criteriaQuery.orderBy(
                criteriaBuilder.desc(root.get(StepExecutionJpa_.id))
        );
        return this.entityManager.createQuery(criteriaQuery).getSingleResult().intValue();
    }

    private int updatePartitionExecution(PartitionExecutionImpl partitionExecutionImpl, boolean notStopping) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<PartitionExecutionJpa> createCriteriaUpdate = criteriaBuilder.createCriteriaUpdate(PartitionExecutionJpa.class);
        Root<PartitionExecutionJpa> root = createCriteriaUpdate.from(PartitionExecutionJpa.class);
        Path<Long> versionPath = root.get(PartitionExecutionJpa_.version);
        createCriteriaUpdate.set(versionPath, criteriaBuilder.sum(versionPath, 1L));
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.batchStatus), partitionExecutionImpl.getBatchStatus());
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.exitStatus), partitionExecutionImpl.getExitStatus());
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.executionException), formatException(partitionExecutionImpl.getException()));
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.persistenUserData), partitionExecutionImpl.getPersistentUserDataSerialized());
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.readerCheckPointInfo), partitionExecutionImpl.getReaderCheckpointInfoSerialized());
        createCriteriaUpdate.set(root.get(PartitionExecutionJpa_.writerCheckPointInfo), partitionExecutionImpl.getWriterCheckpointInfoSerialized());
        createCriteriaUpdate.where(
                notStopping
                        ? criteriaBuilder.and(
                                criteriaBuilder.equal(root.get(PartitionExecutionJpa_.id), partitionExecutionImpl.getPartitionId()),
                                criteriaBuilder.equal(root.get(PartitionExecutionJpa_.stepExecution).get(StepExecutionJpa_.id), partitionExecutionImpl.getStepExecutionId()),
                                criteriaBuilder.notEqual(root.get(PartitionExecutionJpa_.batchStatus), BatchStatus.STOPPED)
                        )
                        : criteriaBuilder.and(
                                criteriaBuilder.equal(root.get(PartitionExecutionJpa_.id), partitionExecutionImpl.getPartitionId()),
                                criteriaBuilder.equal(root.get(PartitionExecutionJpa_.stepExecution).get(StepExecutionJpa_.id), partitionExecutionImpl.getStepExecutionId())
                        )
        );
        return this.entityManager.createQuery(createCriteriaUpdate).executeUpdate();
    }

    @Override
    public void savePersistentData(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution) {
        if (stepOrPartitionExecution instanceof StepExecutionImpl) {
            updateStepExecution(stepOrPartitionExecution, false);
        } else if (stepOrPartitionExecution instanceof PartitionExecutionImpl) {
            updatePartitionExecution(PartitionExecutionImpl.class.cast(stepOrPartitionExecution), false);
        }
    }

    @Override
    public int savePersistentDataIfNotStopping(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution) {
        if (stepOrPartitionExecution instanceof StepExecutionImpl) {
            return updateStepExecution(stepOrPartitionExecution, true);
        } else {
            return updatePartitionExecution(PartitionExecutionImpl.class.cast(stepOrPartitionExecution), true);
        }
    }

}
