/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import java.util.HashSet;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public abstract class IntegerArrayReaderWriterProcessorBase {
    @Inject
    protected StepContext stepContext;

    @Inject
    @BatchProperty(name = "partition.start")
    protected int partitionStart;

    @Inject
    @BatchProperty(name = "partition.end")
    protected Integer partitionEnd;

    /**
     * Specifies the values in the input or output integer array where an {@code ArithmeticException} will be thrown.
     * It does not apply to retry read. But if {@link #repeatFailure} is true, exception is always thrown upon match.
     */
    @Inject
    @BatchProperty(name = "fail.on.values")
    protected Integer[] failOnValues;

    @Inject
    @BatchProperty(name = "repeat.failure")
    protected boolean repeatFailure;

    /**
     * to remember the previous values the failure occurred. During the subsequent retry, the same value should not
     * cause a second failure, unless {@link #repeatFailure} is true.
     */
    protected Set<Integer> failedValues = new HashSet<Integer>();

    protected IntegerArrayReaderWriterProcessorBase() {
        System.out.printf("Instantiating %s%n", this);
    }

    public void close() throws Exception {
    }
}
