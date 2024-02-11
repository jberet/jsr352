/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.infinispanRepositoryRocksDB;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RocksDBInfinispanRepositoryIT extends AbstractIT {
    @BeforeAll
    public static void beforeClass() {
        // skip this test on Windows, see JBERET-500
        // RocksDBInfinispanRepositoryIT.partitionWithInfinispanRocksDB failed on Windows
        Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
    }

    @Test
    public void partitionWithInfinispanRocksDB() throws Exception {
        startJobAndWait(infinispanRepositoryJobXml);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
