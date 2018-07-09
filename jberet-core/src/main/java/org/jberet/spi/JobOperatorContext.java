/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.spi;

import javax.batch.operations.JobOperator;

import org.jberet.operations.JobOperatorImpl;
import org.jberet.util.Assertions;

/**
 * A context on which the {@link JobOperator} can be found.
 * <p>
 * If no {@linkplain #setJobOperatorContextSelector(JobOperatorContextSelector) selector} is set the
 * {@link DefaultJobOperatorContextSelector} will be used. Do note that this does require the implementation to provide
 * a {@link BatchEnvironment} via a {@linkplain java.util.ServiceLoader service loader}.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("unused")
public abstract class JobOperatorContext {

    private static volatile JobOperatorContextSelector SELECTOR = null;

    private static class DefaultHolder {
        // In some cases the BatchEnvironment may not be provided via a ServiceLoader. The
        // DefaultJobOperatorContextSelector assumes a ServiceLoader will be used to find the BatchEnvironment resulting
        // in initialization errors if not found. We get around this by lazily loading the default selector.
        static final JobOperatorContextSelector DEFAULT = new DefaultJobOperatorContextSelector();
    }

    /**
     * Returns the current context based on the selector.
     *
     * @return the current context
     */
    public static JobOperatorContext getJobOperatorContext() {
        JobOperatorContextSelector selector = SELECTOR;
        if (selector == null) {
            selector = DefaultHolder.DEFAULT;
        }
        return selector.getJobOperatorContext();
    }

    /**
     * Creates a new context based on the environment.
     *
     * @param batchEnvironment the batch environment to create the context for, cannot be {@code null}
     *
     * @return the new context
     */
    public static JobOperatorContext create(final BatchEnvironment batchEnvironment) {
        final JobOperator jobOperator = new JobOperatorImpl(Assertions.notNull(batchEnvironment, "batchEnvironment"));
        return new JobOperatorContext() {
            @Override
            public JobOperator getJobOperator() {
                return jobOperator;
            }
        };
    }

    /**
     * Creates a new context which returns the job operator.
     *
     * @param jobOperator the job operator this context should return, cannot be {@code null}
     *
     * @return the new context
     */
    public static JobOperatorContext create(final JobOperator jobOperator) {
        Assertions.notNull(jobOperator, "jobOperator");
        return new JobOperatorContext() {
            @Override
            public JobOperator getJobOperator() {
                return jobOperator;
            }
        };
    }

    /**
     * Allows the selector for the {@link JobOperatorContext} to be set. If the parameter is {@code null} the default
     * selector will be used.
     *
     * @param selector the selector to use
     */
    public static void setJobOperatorContextSelector(final JobOperatorContextSelector selector) {
        SELECTOR = selector;
    }

    /**
     * Returns the {@link JobOperator} for this current context.
     *
     * @return the job operator
     */
    public abstract JobOperator getJobOperator();
}
