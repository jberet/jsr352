/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.operations;

import static org.jberet._private.BatchMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionIsRunningException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.operations.NoSuchJobInstanceException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import org.jberet._private.BatchLogger;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Job;
import org.jberet.operations.FailExecutionsResult.SkippedExecution;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.FailOutcome;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.runner.JobExecutionRunner;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.PropertyKey;

/**
 * An abstract implementation of a {@link JobOperator}. Subclasses should generally delegate to the super methods of
 * this abstraction.
 *
 * @author SPDX-License-Identifier: EPL-2.0
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractJobOperator implements ExtendedJobOperator {

    /**
     * Returns the batch environment that should be used for this JobOperator. Implementations should never return
     * {@code null}.
     *
     * @return the batch environment
     */
    public abstract BatchEnvironment getBatchEnvironment();

    /**
     * This is equivalent to {@link BatchEnvironment#getJobRepository() getBatchEnvironment().getJobRepository()}.
     *
     * @return the job repository that belongs to the batch environment
     */
    public JobRepository getJobRepository() {
        return getBatchEnvironment().getJobRepository();
    }

    @Override
    public long start(final String jobXMLName, final Properties jobParameters) throws JobStartException, JobSecurityException {
        return start(jobXMLName, jobParameters, null);
    }

    /**
     * Creates a new job instance and starts the first execution of that instance.
     *
     * @param jobXMLName    specifies the name of the job XML describing the job
     * @param jobParameters specifies the keyword/value pairs for attribute substitution in the Job XML
     * @param user          the user to associate with the job execution
     *
     * @return executionId for the job execution.
     *
     * @throws JobStartException    if the job failed to start
     * @throws JobSecurityException if there is a security issue with starting the job
     *
     * @see #start(String, Properties)
     */
    public long start(final String jobXMLName, final Properties jobParameters, final String user) throws JobStartException, JobSecurityException {
        final BatchEnvironment batchEnvironment = getBatchEnvironment();
        final Job jobDefined = ArchiveXmlLoader.loadJobXml(jobXMLName, batchEnvironment.getClassLoader(),
                new ArrayList<Job>(), batchEnvironment.getJobXmlResolver());
        return start(jobDefined, jobParameters, user);
    }

    /**
     * Starts a pre-configured {@link Job} instance, with job parameters.
     *
     * @param jobDefined    a pre-configured job
     * @param jobParameters job parameters for the current job execution
     *
     * @return job execution id as a long number
     *
     * @throws JobStartException    if failed to start the job
     * @throws JobSecurityException if failed to start the job due to security permission
     * @see org.jberet.job.model.JobBuilder
     * @since 1.2.0
     */
    public long start(final Job jobDefined, final Properties jobParameters) throws JobStartException, JobSecurityException {
        return start(jobDefined, jobParameters, null);
    }

    /**
     * Starts a pre-configured {@link Job} instance, with job parameters and sets the user on the execution
     *
     * @param jobDefined    a pre-configured job
     * @param jobParameters job parameters for the current job execution
     * @param user          the user to associate with the job execution
     *
     * @return job execution id as a long number
     *
     * @throws JobStartException    if failed to start the job
     * @throws JobSecurityException if failed to start the job due to security permission
     * @see org.jberet.job.model.JobBuilder
     * @since 1.2.2
     */
    public long start(final Job jobDefined, final Properties jobParameters, final String user) throws JobStartException, JobSecurityException {
        final BatchEnvironment batchEnvironment = getBatchEnvironment();
        final ClassLoader classLoader = batchEnvironment.getClassLoader();
        final String applicationName = batchEnvironment.getApplicationName();
        getJobRepository().addJob(new ApplicationAndJobName(applicationName, jobDefined.getId()), jobDefined);
        try {
            return invokeTransaction(new TransactionInvocation<Long>() {
                @Override
                public Long invoke() throws JobStartException, JobSecurityException {
                    final JobInstanceImpl jobInstance = getJobRepository().createJobInstance(jobDefined, applicationName, classLoader);
                    return startJobExecution(jobInstance, jobParameters, null, user);
                }
            });
        } catch (InvalidTransactionException | SystemException e) {
            throw new JobStartException(e);
        }
    }

    @Override
    public void stop(final long executionId) throws NoSuchJobExecutionException,
            JobExecutionNotRunningException, JobSecurityException {
        final JobExecutionImpl jobExecution = getJobExecutionImpl(executionId);
        final BatchStatus s = jobExecution.getBatchStatus();
        if (s == BatchStatus.STOPPED || s == BatchStatus.FAILED || s == BatchStatus.ABANDONED ||
                s == BatchStatus.COMPLETED) {
            throw MESSAGES.jobExecutionNotRunningException(executionId, s);
        } else if (s != BatchStatus.STOPPING) {
            jobExecution.stop();
            if (jobExecution.getStepExecutions().size() == 0) {
                getJobRepository().stopJobExecution(jobExecution);
            }
        }
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        return getJobRepository().getJobNames();
    }

    @Override
    public int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        if (jobName == null) {
            throw MESSAGES.noSuchJobException(null);
        }
        final JobRepository repository = getJobRepository();
        final int count = repository.getJobInstanceCount(jobName);
        if (count == 0 && !repository.jobExists(jobName)) {
            throw MESSAGES.noSuchJobException(jobName);
        }
        return count;
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName, final int start, final int count) throws NoSuchJobException, JobSecurityException {
        if (jobName == null) {
            throw MESSAGES.noSuchJobException(null);
        }
        final JobRepository repository = getJobRepository();
        final List<JobInstance> instances = repository.getJobInstances(jobName);
        final int size = instances.size();
        if (size == 0 && !repository.jobExists(jobName)) {
            throw MESSAGES.noSuchJobException(jobName);
        }
        return instances.subList(Math.min(start, size), Math.min(start + count, size));
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        if (jobName == null) {
            throw MESSAGES.noSuchJobException(null);
        }
        final JobRepository repository = getJobRepository();
        final List<Long> result = repository.getRunningExecutions(jobName);
        if (result.size() == 0 && !repository.jobExists(jobName)) {
            throw MESSAGES.noSuchJobException(jobName);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StaleExecutionsPage listExecutionsNotUpdatedInMinutes(final String jobName, final int minutes,
                                                                 final int start, final int count)
            throws NoSuchJobException, JobSecurityException {
        if (jobName == null) {
            throw MESSAGES.noSuchJobException(null);
        }
        if (minutes < 0) {
            throw MESSAGES.invalidStaleMinutes(minutes);
        }
        if (start < 0 || count <= 0) {
            throw MESSAGES.invalidPagination(start, count);
        }

        final JobRepository repository = getJobRepository();
        //the cut-off comes from this JVM's clock because that is the clock lastUpdatedTime is written from
        final Date notUpdatedSince = new Date(System.currentTimeMillis() - minutes * 60_000L);

        final long total = repository.countStaleExecutions(jobName, notUpdatedSince);
        final List<Long> ids = total == 0 ? Collections.<Long>emptyList()
                : repository.findStaleExecutionIds(jobName, notUpdatedSince, start, count);

        if (total == 0 && ids.isEmpty() && !repository.jobExists(jobName)) {
            //same contract as getRunningExecutions(): an unknown job name is an error, not an empty page
            throw MESSAGES.noSuchJobException(jobName);
        }
        BatchLogger.LOGGER.debugf(
                "Listed stale executions of job %s not updated in %s minutes: start=%s, count=%s, returned=%s, total=%s",
                jobName, minutes, start, count, ids.size(), total);
        return new StaleExecutionsPage(ids, total, start, ids.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FailExecutionsResult failJobExecutions(final List<Long> executionIds) throws JobSecurityException {
        return failJobExecutions(executionIds, FailOptions.defaults());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FailExecutionsResult failJobExecutions(final List<Long> executionIds, final FailOptions options)
            throws JobSecurityException {
        if (executionIds == null) {
            throw MESSAGES.nullVar("executionIds");
        }
        final FailOptions opts = options == null ? FailOptions.defaults() : options;

        // 1. normalise: preserve caller order, collapse duplicates, enforce the cap
        final LinkedHashSet<Long> requested = new LinkedHashSet<Long>(executionIds);
        requested.remove(null);
        if (requested.size() > opts.getMaxBatchSize()) {
            throw MESSAGES.tooManyExecutionsToFail(requested.size(), opts.getMaxBatchSize());
        }
        if (requested.isEmpty()) {
            return FailExecutionsResult.empty();
        }

        final JobRepository repository = getJobRepository();
        final List<SkippedExecution> skipped = new ArrayList<SkippedExecution>();
        final List<Long> candidates = new ArrayList<Long>(requested.size());

        // 2. pre-screen for executions alive in THIS JVM.
        for (final Long id : requested) {
            if (opts.isSkipLocallyRunning() && isRunningLocally(repository, id)) {
                skipped.add(SkippedExecution.runningLocally(id));
                BatchLogger.LOGGER.skippedLocallyRunningExecution(id);
            } else {
                candidates.add(id);
            }
        }
        if (candidates.isEmpty()) {
            return new FailExecutionsResult(Collections.<Long>emptyList(), skipped);
        }

        // 3. the single transaction lives entirely inside the repository
        final Map<Long, FailOutcome> outcomes;
        try {
            outcomes = repository.failJobExecutions(candidates, scopeJobName(), opts.getExitStatus(), new Date());
        } catch (final UnsupportedOperationException e) {
            //a repository that cannot do this at all is a configuration problem, not a transaction failure
            throw e;
        } catch (final RuntimeException e) {
            BatchLogger.LOGGER.failJobExecutionsTransactionFailed(e, candidates, opts.getUser());
            throw new FailJobExecutionsException(candidates, e);
        }

        // 4. translate outcomes into the caller-facing result
        final List<Long> failed = new ArrayList<Long>();
        for (final Long id : candidates) {
            final FailOutcome outcome = FailOutcome.of(outcomes, id);
            switch (outcome.kind()) {
                case FAILED:
                    failed.add(id);
                    break;
                case NOT_IN_SCOPE:
                    skipped.add(SkippedExecution.notInScope(id));
                    break;
                case INVALID_STATUS:
                    skipped.add(SkippedExecution.invalidStatus(id, outcome.observedStatus()));
                    break;
                case CONCURRENTLY_MODIFIED:
                    skipped.add(SkippedExecution.concurrentlyModified(id, outcome.observedStatus()));
                    break;
                case NOT_FOUND:
                default:
                    skipped.add(SkippedExecution.notFound(id));
                    break;
            }
        }
        BatchLogger.LOGGER.failedExecutionsAdministratively(failed, opts.getExitStatus(), opts.getUser(),
                skipped.size());
        return new FailExecutionsResult(failed, skipped);
    }

    /**
     * Returns the job name this operator is scoped to, limiting which executions
     * {@link #failJobExecutions(List, FailOptions)} may touch.
     * <p>
     * In core this is {@code null}, meaning no job-name filter. It is the extension point a
     * container uses to confine an administrative call to the job named in its management address,
     * so that a caller can never fail an execution belonging to a different job.
     *
     * @return the job name to filter on, or {@code null} for an unscoped operator
     *
     * @since 3.2.1
     */
    protected String scopeJobName() {
        return null;
    }

    /**
     * Applies the same liveness heuristic {@link #restart(long, Properties)} uses: a job instance
     * still holding its unsubstituted job model is one whose runtime is live in this JVM.
     * <p>
     * This detects liveness in the current JVM only. An execution running on another node sharing
     * the job repository is indistinguishable from a crashed one from here.
     *
     * @param repository  the job repository
     * @param executionId the job execution id
     *
     * @return true when the execution is detectably running in this JVM
     */
    private boolean isRunningLocally(final JobRepository repository, final long executionId) {
        final JobExecution jobExecution = repository.getJobExecution(executionId);
        if (!(jobExecution instanceof JobExecutionImpl)) {
            return false;
        }
        final JobInstanceImpl jobInstance = ((JobExecutionImpl) jobExecution).getJobInstance();
        return jobInstance != null && jobInstance.getUnsubstitutedJob() != null;
    }

    @Override
    public Properties getParameters(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return getJobExecutionImpl(executionId).getJobParameters();
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters) throws JobExecutionAlreadyCompleteException,
            NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        return restart(executionId, restartParameters, null);
    }

    /**
     * Restarts a stopped or failed job with the specified user.
     *
     * @param executionId       the execution id used for the restart, this must be the most recent execution
     * @param restartParameters the new properties used for the restart
     * @param user              the user to associate with the job execution
     *
     * @return the new execution id
     *
     * @throws JobExecutionAlreadyCompleteException if the job was already completed
     * @throws NoSuchJobExecutionException          the job does not exist
     * @throws JobExecutionNotMostRecentException   if the execution id is not the most recent execution
     * @throws JobRestartException                  of the job failed to restart
     * @throws JobSecurityException                 if failed to start the job due to security permission
     */
    public long restart(final long executionId, final Properties restartParameters, final String user) throws JobExecutionAlreadyCompleteException,
            NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        final JobExecutionImpl originalToRestart = getJobExecutionImpl(executionId);

        if (Job.UNRESTARTABLE.equals(originalToRestart.getRestartPosition())) {
            throw MESSAGES.unrestartableJob(originalToRestart.getJobName(), executionId);
        }

        final BatchStatus previousStatus = originalToRestart.getBatchStatus();
        if (previousStatus == BatchStatus.FAILED || previousStatus == BatchStatus.STOPPED) {
            return restartFailedOrStopped(executionId, originalToRestart, restartParameters, user);
        }

        if (previousStatus == BatchStatus.COMPLETED) {
            throw MESSAGES.jobExecutionAlreadyCompleteException(executionId);
        }

        if (previousStatus == BatchStatus.ABANDONED) {
            throw MESSAGES.jobRestartException(executionId, previousStatus);
        }

        //previousStatus is now one of STARTING, STARTED, or STOPPING
        final String restartMode = restartParameters != null ? restartParameters.getProperty(PropertyKey.RESTART_MODE) : null;
        if (PropertyKey.RESTART_MODE_STRICT.equalsIgnoreCase(restartMode)) {
            throw MESSAGES.jobRestartException(executionId, previousStatus);
        } else if (restartMode == null || restartMode.equalsIgnoreCase(PropertyKey.RESTART_MODE_DETECT)) {
            //to detect if originalToRestart had crashed or not
            if (originalToRestart.getJobInstance().getUnsubstitutedJob() != null) {
                throw MESSAGES.restartRunningExecution(executionId, originalToRestart.getJobName(), previousStatus, restartMode);
            }
        } else if (!restartMode.equalsIgnoreCase(PropertyKey.RESTART_MODE_FORCE)) {
            throw MESSAGES.invalidRestartMode(executionId, originalToRestart.getJobName(), previousStatus, restartMode,
                    Arrays.asList(PropertyKey.RESTART_MODE_DETECT, PropertyKey.RESTART_MODE_FORCE, PropertyKey.RESTART_MODE_STRICT));
        }

        //update batch status in originalToRestart to FAILED, for previousStatus STARTING, STARTED, or STOPPING
        BatchLogger.LOGGER.markAsFailed(executionId, originalToRestart.getJobName(), previousStatus, restartMode);
        originalToRestart.setBatchStatus(BatchStatus.FAILED);
        getJobRepository().updateJobExecution(originalToRestart, false, false);
        return restartFailedOrStopped(executionId, originalToRestart, restartParameters, user);
    }

    @Override
    public void abandon(final long executionId) throws
            NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        final JobExecutionImpl jobExecution = getJobExecutionImpl(executionId);
        final BatchStatus batchStatus = jobExecution.getBatchStatus();
        if (batchStatus == BatchStatus.COMPLETED ||
                batchStatus == BatchStatus.FAILED ||
                batchStatus == BatchStatus.STOPPED ||
                batchStatus == BatchStatus.ABANDONED) {
            jobExecution.setBatchStatus(BatchStatus.ABANDONED);
            getJobRepository().updateJobExecution(jobExecution, false, false);

            final JobInstanceImpl jobInstance = jobExecution.getJobInstance();
            if (jobInstance != null) {
                jobInstance.setUnsubstitutedJob(null);
            }
        } else {
            throw MESSAGES.jobExecutionIsRunningException(executionId);
        }
    }

    @Override
    public JobInstance getJobInstance(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobExecutionImpl jobExecution = getJobExecutionImpl(executionId);
        return jobExecution.getJobInstance();
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance instance) throws
            NoSuchJobInstanceException, JobSecurityException {
        return getJobRepository().getJobExecutions(instance);
    }

    /**
     * Gets job execution ids belonging to the job identified by the {@code jobName}.
     * @param jobName the job name identifying the job
     * @return job execution ids belonging to the job
     * @since 1.3.9.Final, 1.4.3.Final
     */
    public List<Long> getJobExecutionsByJob(final String jobName) {
        return getJobRepository().getJobExecutionsByJob(jobName);
    }

    @Override
    public JobExecution getJobExecution(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return getJobExecutionImpl(executionId);
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) throws
            NoSuchJobExecutionException, JobSecurityException {
        final List<StepExecution> stepExecutions = getJobRepository().getStepExecutions(jobExecutionId, getBatchEnvironment().getClassLoader());
        if (stepExecutions.isEmpty()) {
            //check if the jobExecutionId passed in points to a valid JobExecution
            //since no step executions under this jobExecutionId was found, it's likely this job execution may not exist
            //getJobExecutionImpl() call will throw NoSuchJobExecutionException for non-exist jobExecutionId.
            getJobExecutionImpl(jobExecutionId);
        }
        return stepExecutions;
    }

    /**
     * Returns the job execution implementation found in the job repository.
     *
     * @param executionId the execution id
     *
     * @return the job execution implementation
     *
     * @throws NoSuchJobExecutionException if the job was not found in the repository
     */
    @SuppressWarnings("WeakerAccess")
    protected JobExecutionImpl getJobExecutionImpl(final long executionId) throws NoSuchJobExecutionException {
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobRepository().getJobExecution(executionId);
        if (jobExecution == null) {
            throw MESSAGES.noSuchJobExecution(executionId);
        }
        return jobExecution;
    }

    /**
     * Restarts a FAILED or STOPPED job execution.
     *
     * @param executionId       the old job execution id to restart
     * @param originalToRestart the old job execution
     * @param restartParameters restart job parameters
     * @param user              the user to associate with the job execution
     *
     * @return the new job execution id
     *
     * @throws JobExecutionNotMostRecentException
     * @throws JobRestartException
     */
    private long restartFailedOrStopped(final long executionId, final JobExecutionImpl originalToRestart, final Properties restartParameters, final String user)
            throws JobExecutionNotMostRecentException, JobRestartException {
        final JobInstanceImpl jobInstance = originalToRestart.getJobInstance();
        if (jobInstance == null) {
            throw MESSAGES.noSuchJobInstance(null);
        }
        final List<JobExecution> executions = getJobExecutions(jobInstance);
        if (executions.isEmpty()) {
            throw MESSAGES.noJobExecutionsFoundForRestart(originalToRestart.getJobName(), originalToRestart.getExitStatus(), executionId);
        }
        final JobExecution mostRecentExecution = executions.get(executions.size() - 1);
        if (executionId != mostRecentExecution.getExecutionId()) {
            throw MESSAGES.jobExecutionNotMostRecentException(executionId, jobInstance.getInstanceId(), mostRecentExecution.getExecutionId());
        }
        final BatchEnvironment batchEnvironment = getBatchEnvironment();
        final JobRepository repository = getJobRepository();

        // the job may not have been loaded, e.g., when the restart is performed in a new JVM
        final String jobName = originalToRestart.getJobName();
        Properties oldJobParameters = originalToRestart.getJobParameters();
        Job jobDefined = jobInstance.getUnsubstitutedJob();
        if (jobDefined == null) {
            final ApplicationAndJobName applicationAndJobName = new ApplicationAndJobName(jobInstance.getApplicationName(), jobName);
            jobDefined = repository.getJob(applicationAndJobName);

            if (jobDefined == null) {
                String jobXmlName = null;
                if (oldJobParameters != null) {
                    jobXmlName = oldJobParameters.getProperty(Job.JOB_XML_NAME);
                }
                if (jobXmlName == null) {
                    jobXmlName = jobName;
                } else {
                    oldJobParameters.remove(Job.JOB_XML_NAME);
                    if (!oldJobParameters.propertyNames().hasMoreElements()) {
                        oldJobParameters = null;
                    }
                }
                jobDefined = ArchiveXmlLoader.loadJobXml(jobXmlName, batchEnvironment.getClassLoader(), new ArrayList<Job>(), batchEnvironment.getJobXmlResolver());
                repository.addJob(applicationAndJobName, jobDefined);
            }
            jobInstance.setUnsubstitutedJob(jobDefined);
        }

        try {
            final Properties combinedProperties;
            if (oldJobParameters != null) {
                if (restartParameters == null) {
                    combinedProperties = oldJobParameters;
                } else {
                    combinedProperties = new Properties(oldJobParameters);
                    for (final String k : restartParameters.stringPropertyNames()) {
                        combinedProperties.setProperty(k, restartParameters.getProperty(k));
                    }
                }
            } else {
                combinedProperties = restartParameters;
            }

            return invokeTransaction(new TransactionInvocation<Long>() {
                @Override
                public Long invoke() throws JobStartException, JobSecurityException {
                    return startJobExecution(jobInstance, combinedProperties, originalToRestart, user);
                }
            });
        } catch (final Exception e) {
            throw new JobRestartException(e);
        }
    }

    private long startJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters, final JobExecutionImpl originalToRestart, final String user) throws JobStartException, JobSecurityException {
        final BatchEnvironment batchEnvironment = getBatchEnvironment();
        final JobRepository repository = getJobRepository();
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, jobParameters);
        jobExecution.setUser(user);
        final JobContextImpl jobContext = new JobContextImpl(jobExecution, originalToRestart, new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory()), repository, batchEnvironment);

        final JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        jobContext.getBatchEnvironment().submitTask(jobExecutionRunner);
        return jobExecution.getExecutionId();
    }

    private <T> T invokeTransaction(final TransactionInvocation<T> transactionInvocation) throws SystemException, InvalidTransactionException {
        final TransactionManager tm = getBatchEnvironment().getTransactionManager();
        final Transaction tx = tm.suspend();
        if (tx != null) {
            try {
                return transactionInvocation.invoke();
            } finally {
                tm.resume(tx);
            }
        }
        // No transaction in process
        return transactionInvocation.invoke();
    }

    private interface TransactionInvocation<T> {

        T invoke() throws JobStartException, JobSecurityException;
    }
}
