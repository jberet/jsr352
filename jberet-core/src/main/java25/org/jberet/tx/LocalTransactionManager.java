/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.tx;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jberet._private.BatchLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LocalTransactionManager implements TransactionManager {

    private static class Holder {
        static final LocalTransactionManager INSTANCE = new LocalTransactionManager();
    }

    private final ThreadLocal<Integer> status = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return Status.STATUS_NO_TRANSACTION;
        }
    };

    private LocalTransactionManager() {
    }

    public static LocalTransactionManager getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        status.set(Status.STATUS_ACTIVE);
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        status.set(Status.STATUS_COMMITTED);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        status.set(Status.STATUS_ROLLEDBACK);
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        status.set(Status.STATUS_MARKED_ROLLBACK);
    }

    @Override
    public int getStatus() throws SystemException {
        return status.get();
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return null;
    }

    @Override
    public void setTransactionTimeout(final int seconds) throws SystemException {
        BatchLogger.LOGGER.notImplementedOnLocalTx("setTransactionTimeout");
    }

    @Override
    public Transaction suspend() throws SystemException {
        BatchLogger.LOGGER.notImplementedOnLocalTx("resume");
        return null;
    }

    @Override
    public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
        BatchLogger.LOGGER.notImplementedOnLocalTx("resume");
    }
}
