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

package org.jberet.spi;

import java.io.Serializable;

import org.jberet.runtime.AbstractStepExecution;

public interface PartitionWorker {
    void reportData(Serializable data, AbstractStepExecution partitionExecution) throws Exception;

    void partitionDone(AbstractStepExecution partitionExecution) throws Exception;
}
