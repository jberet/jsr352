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
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jberet._private.BatchLogger;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
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
public abstract class AbstractJobOperator implements JobOperator {

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
        final JobExecution mostRecentExecution = executions.get(executions.size() - 1);
        if (executionId != mostRecentExecution.getExecutionId()) {
            throw MESSAGES.jobExecutionNotMostRecentException(executionId, jobInstance.getInstanceId());
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
