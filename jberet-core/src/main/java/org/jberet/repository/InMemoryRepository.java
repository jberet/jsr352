/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;

public final class InMemoryRepository extends AbstractRepository {
    private final ConcurrentMap<Long, JobInstanceImpl> jobInstances = new ConcurrentHashMap<Long, JobInstanceImpl>();
    private final ConcurrentMap<Long, JobExecutionImpl> jobExecutions = new ConcurrentHashMap<Long, JobExecutionImpl>();

    private final AtomicLong jobInstanceIdSequence = new AtomicLong();
    private final AtomicLong jobExecutionIdSequence = new AtomicLong();
    private final AtomicLong stepExecutionIdSequence = new AtomicLong();

    public InMemoryRepository() {
    }

    private static class Holder {
        private static final InMemoryRepository instance = new InMemoryRepository();
    }

    /**
     * Gets a singleton instance of an in-memory job repository.
     *
     * @return an in-memory job repository
     */
    public static InMemoryRepository getInstance() {
        return Holder.instance;
    }

    /**
     * Creates a new in-memory job repository.
     * Use where multiple in-memory job repositories may be needed.
     *
     * @return a new in-memory job repository
     */
    public static InMemoryRepository create() {
        return new InMemoryRepository();
    }

    @Override
    public void removeJob(final String jobId) {
        super.removeJob(jobId);

        //perform cascade delete
        for (final Iterator<Map.Entry<Long, JobInstanceImpl>> it = jobInstances.entrySet().iterator(); it.hasNext(); ) {
            final JobInstance ji = it.next().getValue();
            if (ji.getJobName().equals(jobId)) {
                BatchLogger.LOGGER.removing(JobInstance.class.getName(), String.valueOf(ji.getInstanceId()));
                it.remove();
            }
        }

        for (final Iterator<Map.Entry<Long, JobExecutionImpl>> it = jobExecutions.entrySet().iterator(); it.hasNext(); ) {
            final JobExecution je = it.next().getValue();
            if (je.getJobName().equals(jobId)) {
                if (je.getJobParameters() != null) {
                    je.getJobParameters().clear();
                }
                BatchLogger.LOGGER.removing(JobExecution.class.getName(), String.valueOf(je.getExecutionId()));
                it.remove();
            }
        }
    }

    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        jobInstance.setId(jobInstanceIdSequence.incrementAndGet());
    }

    @Override
    void insertJobExecution(final JobExecutionImpl jobExecution) {
        jobExecution.setId(jobExecutionIdSequence.incrementAndGet());
    }

    @Override
    void insertStepExecution(final StepExecutionImpl stepExecution, final JobExecutionImpl jobExecution) {
        stepExecution.setId(stepExecutionIdSequence.incrementAndGet());
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        // do nothing
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        int count = 0;
        final JobInstanceImpl jobInstanceImpl = jobInstances.get(jobInstanceId);
        if (jobInstanceImpl != null) {
            for (final JobExecution jobExecution : jobInstanceImpl.getJobExecutions()) {
                final JobExecutionImpl jobExecutionImpl = (JobExecutionImpl) jobExecution;
                for (final StepExecution stepExecution : jobExecutionImpl.getStepExecutions()) {
                    if (stepExecution.getStepName().equals(stepName)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public void removeJobExecutions(final JobExecutionSelector jobExecutionSelector) {
        final Collection<Long> allJobExecutionIds = jobExecutions.keySet();
        for (final Iterator<Map.Entry<Long, JobExecutionImpl>> it = jobExecutions.entrySet().iterator(); it.hasNext(); ) {
            final JobExecutionImpl je = it.next().getValue();
            if (jobExecutionSelector == null || jobExecutionSelector.select(je, allJobExecutionIds)) {
                if (je.getJobParameters() != null) {
                    je.getJobParameters().clear();
                }
                BatchLogger.LOGGER.removing(JobExecution.class.getName(), String.valueOf(je.getExecutionId()));
                it.remove();
            }
        }
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        final JobInstanceImpl jobInstance = new JobInstanceImpl(job, applicationName, job.getId());
        insertJobInstance(jobInstance);
        jobInstances.put(jobInstance.getInstanceId(), jobInstance);
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceIdToRemove) {
        BatchLogger.LOGGER.removing(JobInstance.class.getName(), String.valueOf(jobInstanceIdToRemove));
        jobInstances.remove(jobInstanceIdToRemove);
    }

    @Override
    public JobInstance getJobInstance(final long jobInstanceId) {
        return jobInstances.get(jobInstanceId);
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        final List<JobInstance> result = new ArrayList<JobInstance>();
        final long largestJobInstanceId = jobInstanceIdSequence.get();

        final boolean selectAll = jobName == null || jobName.equals("*");
        for (long i = largestJobInstanceId; i > 0; i--) {
            final JobInstanceImpl e = jobInstances.get(i);
            if (e != null && (selectAll || jobName.equals(e.getJobName()))) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        int count = 0;
        for (final JobInstance e : jobInstances.values()) {
            if (e.getJobName().equals(jobName)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        final JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        insertJobExecution(jobExecution);
        jobExecutions.put(jobExecution.getExecutionId(), jobExecution);
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        return jobExecutions.get(jobExecutionId);
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        if (jobInstance == null) {
            //return all JobExecution
            final List<JobExecution> result = new ArrayList<JobExecution>();
            result.addAll(this.jobExecutions.values());
            return result;
        } else {
            return ((JobInstanceImpl) jobInstance).getJobExecutions();
        }
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        final JobExecutionImpl jobExecution = getJobExecution(jobExecutionId);
        if (jobExecution == null) {
            return Collections.emptyList();
        }
        return jobExecution.getStepExecutions();
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart,
                                                                 final ClassLoader classLoader) {
        for (final StepExecution stepExecution : jobExecutionToRestart.getStepExecutions()) {
            if (stepName.equals(stepExecution.getStepName())) {
                return (StepExecutionImpl) stepExecution;
            }
        }
        StepExecutionImpl result = null;
        // the same-named StepExecution is not found in the jobExecutionToRestart.  It's still possible the same-named
        // StepExecution may exit in JobExecution earlier than jobExecutionToRestart for the same JobInstance.
        final long instanceId = jobExecutionToRestart.getJobInstance().getInstanceId();
        for (final JobExecutionImpl jobExecutionImpl : jobExecutions.values()) {
            //skip the JobExecution that has already been checked above
            if (instanceId == jobExecutionImpl.getJobInstance().getInstanceId() &&
                    jobExecutionImpl.getExecutionId() != jobExecutionToRestart.getExecutionId()) {
                for (final StepExecution stepExecution : jobExecutionImpl.getStepExecutions()) {
                    if (stepExecution.getStepName().equals(stepName)) {
                        if (result == null || result.getStepExecutionId() < stepExecution.getStepExecutionId()) {
                            result = (StepExecutionImpl) stepExecution;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        final List<Long> result = new ArrayList<Long>();

        for (final Map.Entry<Long, JobExecutionImpl> e : jobExecutions.entrySet()) {
            if (e.getValue().getJobName().equals(jobName)) {
                final BatchStatus s = e.getValue().getBatchStatus();
                if (s == BatchStatus.STARTING || s == BatchStatus.STARTED) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getJobExecutionsByJob(String jobName) {
        return jobExecutions.values().stream().filter(e -> e.getJobName().equals(jobName))
                .map(JobExecution::getExecutionId)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
}
