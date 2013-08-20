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

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;

public final class InMemoryRepository extends AbstractRepository {
    private final AtomicLong jobInstanceIdSequence = new AtomicLong();
    private final AtomicLong jobExecutionIdSequence = new AtomicLong();
    private final AtomicLong stepExecutionIdSequence = new AtomicLong();

    private InMemoryRepository() {
    }

    private static class Holder {
        private static final InMemoryRepository instance = new InMemoryRepository();
    }

    static InMemoryRepository getInstance() {
        return Holder.instance;
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
}
