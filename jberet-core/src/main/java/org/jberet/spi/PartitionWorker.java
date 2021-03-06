/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.spi;

import java.io.Serializable;

import org.jberet.runtime.AbstractStepExecution;

public interface PartitionWorker {
    void reportData(Serializable data, AbstractStepExecution partitionExecution) throws Exception;

    void partitionDone(AbstractStepExecution partitionExecution) throws Exception;
}
