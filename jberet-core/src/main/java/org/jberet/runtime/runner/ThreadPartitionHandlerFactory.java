/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.runner;

import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.spi.PartitionHandler;
import org.jberet.spi.PartitionHandlerFactory;

public class ThreadPartitionHandlerFactory implements PartitionHandlerFactory {

    private static final ThreadPartitionHandlerFactory instance = new ThreadPartitionHandlerFactory();

    public static ThreadPartitionHandlerFactory getInstance() {
        return instance;
    }

    @Override
    public PartitionHandler createPartitionHandler(final PartitionExecutionImpl partitionExecution,
                                                   final StepExecutionRunner stepExecutionRunner) {
        return new ThreadPartitionHandler(partitionExecution, stepExecutionRunner);
    }
}
