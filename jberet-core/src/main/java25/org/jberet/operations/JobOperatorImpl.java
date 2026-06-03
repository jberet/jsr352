/*
 * Copyright (c) 2012-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.operations;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.operations.JobOperator;

import org.jberet._private.BatchMessages;
import org.jberet.repository.JobRepository;
import org.jberet.spi.BatchEnvironment;

public class JobOperatorImpl extends AbstractJobOperator implements JobOperator {

    final JobRepository repository;
    private final BatchEnvironment batchEnvironment;

    public JobOperatorImpl() throws BatchRuntimeException {
        this(SecurityActions.loadBatchEnvironment());
    }

    public JobOperatorImpl(final BatchEnvironment batchEnvironment) throws BatchRuntimeException {
        if (batchEnvironment == null) {
            throw BatchMessages.MESSAGES.batchEnvironmentNotFound();
        }
        this.batchEnvironment = batchEnvironment;
        repository = this.batchEnvironment.getJobRepository();
        if (repository == null) {
            throw BatchMessages.MESSAGES.jobRepositoryRequired();
        }
    }

    @Override
    public BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }
}
