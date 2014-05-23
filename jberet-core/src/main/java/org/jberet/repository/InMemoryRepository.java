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

package org.jberet.repository;

import java.util.concurrent.atomic.AtomicLong;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;

public final class InMemoryRepository extends AbstractRepository {
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
     * <p/>
     * Use where multiple in-memory job repositories may be needed.
     *
     * @return a new in-memory job repository
     */
    public static InMemoryRepository create() {
        return new InMemoryRepository();
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
        final JobInstanceImpl jobInstanceImpl = (JobInstanceImpl) jobInstances.get(jobInstanceId);
        if (jobInstanceImpl != null) {
            for(final JobExecution jobExecution : jobInstanceImpl.getJobExecutions()) {
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
}
