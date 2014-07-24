/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
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

import org.jberet._private.BatchMessages;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.ArtifactFactoryWrapper;
import org.jberet.job.model.Job;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.runner.JobExecutionRunner;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
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
            batchEnvironment = WildFlySecurityManager.doUnchecked(loaderAction);
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
        final ClassLoader classLoader = batchEnvironment.getClassLoader();
        final Job jobDefined = ArchiveXmlLoader.loadJobXml(jobXMLName, classLoader, new ArrayList<Job>());
        repository.addJob(jobDefined);
        try {
            return invokeTransaction(new TransactionInvocation<Long>() {
                @Override
                public Long invoke() throws JobStartException, JobSecurityException {
                    final JobInstanceImpl jobInstance = repository.createJobInstance(jobDefined, getApplicationName(), classLoader);
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
        final JobExecutionImpl jobExecution = (JobExecutionImpl) repository.getJobExecution(executionId);
        if (jobExecution == null) {
            throw MESSAGES.noSuchJobExecution(executionId);
        }
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
        final Set<String> result = new HashSet<String>();
        for (final Job e : repository.getJobs()) {
            result.add(e.getId());
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        final int count = repository.getJobInstanceCount(jobName);
        if (count == 0) {
            throw MESSAGES.noSuchJobException(jobName);
        }
        return count;
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName, final int start, final int count) throws NoSuchJobException, JobSecurityException {
        final List<JobInstance> result = new ArrayList<JobInstance>();
        int pos = 0;
        final List<JobInstance> instances = repository.getJobInstances(jobName);
        for (int i = instances.size() - 1; i >= 0; i--) {
            final JobInstance e = instances.get(i);
            if (pos >= start) {
                if (result.size() < count) {
                    result.add(e);
                } else {
                    break;
                }
            }
            pos++;
        }
        if (pos == 0) {
            throw MESSAGES.noSuchJobException(jobName);
        }
        return result;
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        final List<Long> result = new ArrayList<Long>();
        boolean jobExists = false;
        for (final JobExecution e : repository.getJobExecutions(null)) {
            if (e.getJobName().equals(jobName)) {
                jobExists = true;
                final BatchStatus s = e.getBatchStatus();
                if (s == BatchStatus.STARTING || s == BatchStatus.STARTED) {
                    result.add(e.getExecutionId());
                }
            }
        }
        if (!jobExists) {
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
        long newExecutionId = 0;
        final JobExecutionImpl originalToRestart = (JobExecutionImpl) getJobExecution(executionId);
        if (originalToRestart == null) {
            throw MESSAGES.noSuchJobExecution(executionId);
        }
        final BatchStatus previousStatus = originalToRestart.getBatchStatus();
        if (previousStatus == BatchStatus.COMPLETED) {
            throw MESSAGES.jobExecutionAlreadyCompleteException(executionId);
        }
        if (previousStatus == BatchStatus.ABANDONED ||
                previousStatus == BatchStatus.STARTED ||
                previousStatus == BatchStatus.STARTING ||
                previousStatus == BatchStatus.STOPPING) {
            throw MESSAGES.jobRestartException(executionId, previousStatus);
        }
        if (previousStatus == BatchStatus.FAILED || previousStatus == BatchStatus.STOPPED) {
            final JobInstanceImpl jobInstance = (JobInstanceImpl) getJobInstance(executionId);
            final List<JobExecution> executions = getJobExecutions(jobInstance);
            final JobExecution mostRecentExecution = executions.get(executions.size() - 1);
            if (executionId != mostRecentExecution.getExecutionId()) {
                throw MESSAGES.jobExecutionNotMostRecentException(executionId, jobInstance.getInstanceId());
            }

            // the job may not have been loaded, e.g., when the restart is performed in a new JVM
            final String jobName = originalToRestart.getJobName();
            if (repository.getJob(jobName) == null) {
                final Job jobDefined = ArchiveXmlLoader.loadJobXml(jobName, batchEnvironment.getClassLoader(), new ArrayList<Job>());
                repository.addJob(jobDefined);
            }
            if (jobInstance.getUnsubstitutedJob() == null) {
                jobInstance.setUnsubstitutedJob(repository.getJob(jobName));
            }
            try {
                newExecutionId = invokeTransaction(new TransactionInvocation<Long>() {
                    @Override
                    public Long invoke() throws JobStartException, JobSecurityException {
                        return startJobExecution(jobInstance, restartParameters, originalToRestart);
                    }
                });
            } catch (Exception e) {
                throw new JobRestartException(e);
            }
        }
        return newExecutionId;
    }

    @Override
    public void abandon(final long executionId) throws
            NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        final JobExecutionImpl jobExecution = (JobExecutionImpl) getJobExecution(executionId);
        if (jobExecution == null) {
            throw MESSAGES.noSuchJobExecution(executionId);
        }
        final BatchStatus batchStatus = jobExecution.getBatchStatus();
        if (batchStatus == BatchStatus.COMPLETED ||
                batchStatus == BatchStatus.FAILED ||
                batchStatus == BatchStatus.STOPPED ||
                batchStatus == BatchStatus.ABANDONED) {
            jobExecution.setBatchStatus(BatchStatus.ABANDONED);
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
        return repository.getJobExecutions(instance);
    }

    @Override
    public JobExecution getJobExecution(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return repository.getJobExecution(executionId);
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) throws
            NoSuchJobExecutionException, JobSecurityException {
        return repository.getStepExecutions(jobExecutionId);
    }

    private long startJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters, final JobExecutionImpl originalToRestart) throws JobStartException, JobSecurityException {
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, jobParameters);
        final JobContextImpl jobContext = new JobContextImpl(jobExecution, originalToRestart, artifactFactory, repository, batchEnvironment);

        final JobExecutionRunner jobExecutionRunner = new JobExecutionRunner(jobContext);
        jobContext.getBatchEnvironment().submitTask(jobExecutionRunner);
        final long jobExecutionId = jobExecution.getExecutionId();
        return jobExecutionId;
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
