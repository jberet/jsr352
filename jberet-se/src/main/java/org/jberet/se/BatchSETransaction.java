/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.se;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jberet.se._private.SEBatchLogger;

import static javax.transaction.Status.*;

/**
 * An implementation of {@code javax.transaction.UserTransaction} in Java SE environment for batch job execution.  It
 * merely updates the transaction status for each transaction operations on each thread.
 */
public final class BatchSETransaction implements UserTransaction {
    private static final BatchSETransaction instance = new BatchSETransaction();

    private static final ThreadLocal<Integer> status = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return STATUS_NO_TRANSACTION;
        }
    };

    private BatchSETransaction() {
    }

    static BatchSETransaction getInstance() {
        return instance;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        status.set(STATUS_ACTIVE);
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        status.set(STATUS_COMMITTED);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        status.set(STATUS_ROLLEDBACK);
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        status.set(STATUS_MARKED_ROLLBACK);
    }

    @Override
    public int getStatus() throws SystemException {
        return status.get();
    }

    @Override
    public void setTransactionTimeout(final int seconds) throws SystemException {
        SEBatchLogger.LOGGER.methodDoesNothing(BatchSETransaction.class.getName(), "setTransactionTimeout");
    }
}
