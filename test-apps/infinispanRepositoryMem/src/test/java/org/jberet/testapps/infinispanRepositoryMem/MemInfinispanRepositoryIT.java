/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.infinispanRepositoryMem;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemInfinispanRepositoryIT extends AbstractIT {
    @Test
    public void partitionWithInfinispanMem() throws Exception {
        startJobAndWait(infinispanRepositoryJobXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
