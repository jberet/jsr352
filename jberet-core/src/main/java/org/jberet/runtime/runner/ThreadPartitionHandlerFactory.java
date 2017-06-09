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

import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.PartitionHandler;
import org.jberet.spi.PartitionHandlerFactory;

public class ThreadPartitionHandlerFactory implements PartitionHandlerFactory {

    private static final ThreadPartitionHandlerFactory instance = new ThreadPartitionHandlerFactory();

    private ThreadPartitionHandlerFactory() {
    }

    public static ThreadPartitionHandlerFactory getInstance() {
        return instance;
    }

    @Override
    public PartitionHandler createPartitionHandler(final StepContextImpl stepContext,
                                                   final StepExecutionRunner stepExecutionRunner) {
        return new ThreadPartitionHandler(stepExecutionRunner);
    }
}
