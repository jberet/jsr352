/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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
