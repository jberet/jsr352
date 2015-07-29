/*
 * Copyright (c) 2012-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.operations;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import javax.batch.operations.BatchRuntimeException;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.runner.JobExecutionRunner;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.PropertyKey;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jberet._private.BatchMessages.MESSAGES;

public class JobOperatorImpl implements JobOperator {

    private static final PrivilegedAction<BatchEnvironment> loaderAction = new PrivilegedAction<BatchEnvironment>() {
        @Override
        public BatchEnvironment run() {
            final ServiceLoader<BatchEnvironment> serviceLoader = ServiceLoader.load(BatchEnvironment.class);
            if (serviceLoader.iterator().hasNext()) {
                return serviceLoader.iterator().next();
            }
            return null;
        }
    };

    final JobRepository repository;
    private final BatchEnvironment batchEnvironment;
    private final ArtifactFactory artifactFactory;

    public JobOperatorImpl() throws BatchRuntimeException {
        final BatchEnvironment batchEnvironment;
        if (WildFlySecurityManager.isChecking()) {
            batchEnvironment = AccessController.doPrivileged(loaderAction);
        } else {
            batchEnvironment = loaderAction.run();
        }

        if (batchEnvironment == null) {
            throw BatchMessages.MESSAGES.batchEnvironmentNotFound();
        }
        this.batchEnvironment = batchEnvironment;
        artifactFactory = new ArtifactFactoryWrapper(batchEnvironment.getArtifactFactory());
        repository = batchEnvironment.getJobRepository();
        if (repository == null) {
            throw BatchMessages.MESSAGES.jobRepositoryRequired();
        }
    }

    @Override
    public long start(final String jobXMLName, final Properties jobParameters) throws JobStartException, JobSecurityException {
        final Job jobDefined = ArchiveXmlLoader.loadJobXml(jobXMLName, batchEnvironment.getClassLoader(),
                new ArrayList<Job>(), batchEnvironment.getJobXmlResolver());
        return start(jobDefined, jobParameters);
    }

    /**
     * Starts a pre-configured {@link Job} instance, with job parameters.
     *
     * @param jobDefined a pre-configured job
     * @param jobParameters job parameters for the current job execution
     * @return job execution id as a long number
     * @throws JobStartException if failed to start the job
     * @throws JobSecurityException if failed to start the job due to security permission
     *
     * @see org.jberet.job.model.JobBuilder
     * @since 1.2.0
     */
    public long start(final Job jobDefined, final Properties jobParameters) throws JobStartException, JobSecurityException {
        final ClassLoader classLoader = batchEnvironment.getClassLoader();
        final String applicationName = getApplicationName();
        repository.addJob(new ApplicationAndJobName(applicationName, jobDefined.getId()), jobDefined);
        try {
            return invokeTransaction(new TransactionInvocation<Long>() {
                @Override
                public Long invoke() throws JobStartException, JobSecurityException {
                    final JobInstanceImpl jobInstance = repository.createJobInstance(jobDefined, applicationName, classLoader);
                    return startJobExecution(jobInstance, jobParameters, null);
                }
            });
        } catch (InvalidTransactionException e) {
            throw new JobStartException(e);
        } catch (SystemException e) {
            throw new JobStartException(e);
        }
    }

    @Override
    public void stop(final long executionId) throws NoSuchJobExecutionException,
            JobExecutionNotRunningException, JobSecurityException {
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        final BatchStatus s = jobExecution.getBatchStatus();
        if (s == BatchStatus.STOPPED || s == BatchStatus.FAILED || s == BatchStatus.ABANDONED ||
                s == BatchStatus.COMPLETED) {
            throw MESSAGES.jobExecutionNotRunningException(executionId, s);
        } else if (s == BatchStatus.STOPPING) {
            //in process of stopping, do nothing
        } else {
            jobExecution.setBatchStatus(BatchStatus.STOPPING);
            jobExecution.stop();
        }
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        return repository.getJobNames();
    }

    @Override
    public int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        if (jobName == null) {
            throw MESSAGES.noSuchJobException(null);
        }
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
        final List<Long> result = repository.getRunningExecutions(jobName);
        if (result.size() == 0 && !repository.jobExists(jobName)) {
            throw MESSAGES.noSuchJobException(jobName);
        }
        return result;
    }

    @Override
    public Properties getParameters(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return getJobExecution(executionId).getJobParameters();
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters) throws JobExecutionAlreadyCompleteException,
            NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        final JobExecutionImpl originalToRestart = (JobExecutionImpl) getJobExecution(executionId);

        if (Job.UNRESTARTABLE.equals(originalToRestart.getRestartPosition())) {
            throw MESSAGES.unrestartableJob(originalToRestart.getJobName(), executionId);
        }

        final BatchStatus previousStatus = originalToRestart.getBatchStatus();
        if (previousStatus == BatchStatus.FAILED || previousStatus == BatchStatus.STOPPED) {
            return restartFailedOrStopped(executionId, originalToRestart, restartParameters);
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
        } else if(restartMode == null || restartMode.equalsIgnoreCase(PropertyKey.RESTART_MODE_DETECT)) {
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
        repository.updateJobExecution(originalToRestart, false, false);
        return restartFailedOrStopped(executionId, originalToRestart, restartParameters);
    }

    @Override
    public void abandon(final long executionId) throws
            NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        final BatchStatus batchStatus = jobExecution.getBatchStatus();
        if (batchStatus == BatchStatus.COMPLETED ||
                batchStatus == BatchStatus.FAILED ||
                batchStatus == BatchStatus.STOPPED ||
                batchStatus == BatchStatus.ABANDONED) {
            jobExecution.setBatchStatus(BatchStatus.ABANDONED);
            repository.updateJobExecution(jobExecution, false, false);

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
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        return jobExecution.getJobInstance();
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance instance) throws
            NoSuchJobInstanceException, JobSecurityException {
        if (instance == null) {
            throw MESSAGES.noSuchJobInstance(null);
        }
        return repository.getJobExecutions(instance);
    }

    @Override
    public JobExecution getJobExecution(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        final JobExecution jobExecution = repository.getJobExecution(executionId);
        if (jobExecution == null) {
            throw MESSAGES.noSuchJobExecution(executionId);
        }
        return jobExecution;
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) throws
            NoSuchJobExecutionException, JobSecurityException {
        final List<StepExecution> stepExecutions = repository.getStepExecutions(jobExecutionId, batchEnvironment.getClassLoader());
        if (stepExecutions.isEmpty()) {
            //check if the jobExecutionId passed in points to a valid JobExecution
            //since no step executions under this jobExecutionId was found, it's likely this job execution may not exist
            //getJobExecution() call will throw NoSuchJobExecutionException for non-exist jobExecutionId.
            getJobExecution(jobExecutionId);
        }
        return stepExecutions;
    }

    /**
     * Restarts a FAILED or STOPPED job execution.
     *
     * @param executionId the old job execution id to restart
     * @param originalToRestart the old job execution
     * @param restartParameters restart job parameters
     * @return the new job execution id
     * @throws JobExecutionNotMostRecentException
     * @throws JobRestartException
     */
    private long restartFailedOrStopped(final long executionId, final JobExecutionImpl originalToRestart, final Properties restartParameters)
            throws JobExecutionNotMostRecentException, JobRestartException {
        final JobInstanceImpl jobInstance = originalToRestart.getJobInstance();
        final List<JobExecution> executions = getJobExecutions(jobInstance);
        final JobExecution mostRecentExecution = executions.get(executions.size() - 1);
        if (executionId != mostRecentExecution.getExecutionId()) {
            throw MESSAGES.jobExecutionNotMostRecentException(executionId, jobInstance.getInstanceId());
        }

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
                    return startJobExecution(jobInstance, combinedProperties, originalToRestart);
                }
            });
        } catch (final Exception e) {
            throw new JobRestartException(e);
        }
    }

    private long startJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters, final JobExecutionImpl originalToRestart) throws JobStartException, JobSecurityException {
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, jobParameters);
        final JobContextImpl jobContext = new JobContextImpl(jobExecution, originalToRestart, artifactFactory, repository, batchEnvironment);

        final JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        jobContext.getBatchEnvironment().submitTask(jobExecutionRunner);
        return jobExecution.getExecutionId();
    }

    private String getApplicationName() {
        try {
            return InitialContext.doLookup("java:app/AppName");
        } catch (NamingException e) {
            return null;
        }
    }

    private <T> T invokeTransaction(final TransactionInvocation<T> transactionInvocation) throws SystemException, InvalidTransactionException {
        final TransactionManager tm = batchEnvironment.getTransactionManager();
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

    private static interface TransactionInvocation<T> {

        T invoke() throws JobStartException, JobSecurityException;
    }
}
