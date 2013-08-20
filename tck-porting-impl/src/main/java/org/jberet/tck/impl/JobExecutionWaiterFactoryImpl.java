/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.tck.impl;

import javax.batch.operations.JobOperator;

import com.ibm.jbatch.tck.spi.JobExecutionWaiter;
import com.ibm.jbatch.tck.spi.JobExecutionWaiterFactory;

public final class JobExecutionWaiterFactoryImpl implements JobExecutionWaiterFactory {
    @Override
    public JobExecutionWaiter createWaiter(final long executionId, final JobOperator jobOp, final long sleepTime) {
        return new JobExecutionWaiterImpl(executionId, jobOp, sleepTime);
    }
}
