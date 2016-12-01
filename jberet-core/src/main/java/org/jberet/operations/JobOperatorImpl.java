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
import java.util.ServiceLoader;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobOperator;

import org.jberet._private.BatchMessages;
import org.jberet.repository.JobRepository;
import org.jberet.spi.BatchEnvironment;
import org.wildfly.security.manager.WildFlySecurityManager;

public class JobOperatorImpl extends AbstractJobOperator implements JobOperator {

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

    public JobOperatorImpl() throws BatchRuntimeException {
        this(WildFlySecurityManager.isChecking() ? AccessController.doPrivileged(loaderAction) : loaderAction.run());
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
    protected BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }
}
