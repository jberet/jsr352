/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.spi;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import org.jberet._private.BatchMessages;

/**
 * An executor service to be used with batch jobs. Implementations of JBeret should use this to execute batch tasks.
 * Some tasks require special handling and therefore need to be queued before they can be run in some circumstances.
 * <p>
 * Partition jobs require some special handling with executors. Extending this in implementations will give the desired
 * behavior to the implementing class. Note that {@link #getMaximumPoolSize()} should return a value greater than 2.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class JobExecutor implements Executor {
    private final ReentrantLock lock;
    private final Executor delegate;
    private final Deque<JobTask> queuedTasks;
    private int usedPermits;

    /**
     * Creates a new executor.
     *
     * @param delegate the executor that tasks should be submitted to when they are able to run
     */
    protected JobExecutor(final Executor delegate) {
        lock = new ReentrantLock(true);
        this.delegate = delegate;
        queuedTasks = new ArrayDeque<JobTask>();
        usedPermits = 0;
    }

    /**
     * Returns the maximum number of threads allowed to be executed. The value returned must be greater than 0.
     *
     * @return the maximum number of threads allowed to be executed
     */
    protected abstract int getMaximumPoolSize();

    @Override
    public final void execute(final Runnable runnable) {
        execute(wrap(runnable), false);
    }

    /**
     * Executes the given task at some time in the future.
     *
     * <p>
     * If the {@link JobTask#getRequiredRemainingPermits()} is any value greater than {@code 0} then the task may be
     * queued for later execution if the number of permits used is greater than the maximum permits allowed plus the
     * number of remaining permits required.
     * </p>
     *
     * <p>
     * As an example if the maximum number of permits allowed is 5 and 4 threads are executing an invocation with 2
     * {@link JobTask#getRequiredRemainingPermits()} would be queued.
     * </p>
     *
     * @param task the task to run
     */
    public final void execute(final JobTask task) {
        execute(task, false);
    }

    private void execute(final JobTask task, final boolean reentry) {
        final int requiredRemainingPermits = (task.getRequiredRemainingPermits() < 0 ? 0 : task.getRequiredRemainingPermits());
        Runnable r = null;
        final int maxPermits = getMaximumPoolSize();
        if (requiredRemainingPermits > maxPermits) {
            throw BatchMessages.MESSAGES.insufficientPermits(requiredRemainingPermits, maxPermits);
        }
        lock.lock();
        try {
            // Compare the max permits allowed and the number of used permits, queue if the requiredRemainPermits is
            // greater than the max permits, otherwise execute the task. If all permits can be used,
            // requiredRemainingPermits == 0, then just use the delegate executor to queue the tasks as it sees fit.
            // Note that if this executor needs to act as a thread-pool then two arrays will be needed for tasks that
            // require permits to stay open and tasks that don't require permits to stay open.
            if ((usedPermits + requiredRemainingPermits) <= maxPermits || requiredRemainingPermits == 0) {
                ++usedPermits;
                r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            task.run();
                        } finally {
                            release();
                        }
                    }
                };
            } else {
                // If the entry was already from the queue, but there's not enough permits add the task to the top of
                // the queue
                if (reentry) {
                    queuedTasks.addFirst(task);
                } else {
                    queuedTasks.add(task);
                }
            }
        } finally {
            lock.unlock();
        }
        if (r != null) {
            delegate.execute(r);
        }
    }

    private void release() {
        final JobTask next;
        lock.lock();
        try {
            --usedPermits;
            if (usedPermits < 0) usedPermits = 0;
            next = queuedTasks.poll();
        } finally {
            lock.unlock();
        }
        if (next != null) {
            execute(next, true);
        }
    }

    /**
     * Wraps the original {@code java.lang.Runnable} to a {@code org.jberet.spi.JobTask},
     * adding appropriate implementation of {@code org.jberet.spi.JobTask#getRequiredRemainingPermits()}
     * method.
     * <p>
     * Subclass may override this method to wrap it appropriate for its environment.
     *
     * @param task the original {@code Runnable}
     * @return a {@code JobTask} wrapping the original {@code Runnable}
     */
    protected JobTask wrap(final Runnable task) {
        if (task instanceof JobTask) {
            return (JobTask) task;
        }
        return new JobTask() {
            @Override
            public int getRequiredRemainingPermits() {
                return 0;
            }

            @Override
            public void run() {
                task.run();
            }
        };
    }
}
