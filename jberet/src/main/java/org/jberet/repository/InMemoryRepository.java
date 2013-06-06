/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012-2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    void insertJobInstance(JobInstanceImpl jobInstance) {
        jobInstance.setId(jobInstanceIdSequence.incrementAndGet());
    }

    @Override
    void insertJobExecution(JobExecutionImpl jobExecution) {
        jobExecution.setId(jobExecutionIdSequence.incrementAndGet());
    }

    @Override
    void insertStepExecution(StepExecutionImpl stepExecution, JobExecutionImpl jobExecution) {
        stepExecution.setId(stepExecutionIdSequence.incrementAndGet());
    }
}
