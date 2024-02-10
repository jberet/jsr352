/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.infinispanRepositoryFile;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class FileInfinispanRepositoryIT extends AbstractIT {
	
	static {
		System.setProperty("jberet.infinispan.file.store", System.getProperty("jberet.tmp.dir"));
	}
	
    @Test
    public void partitionWithInfinispanFile() throws Exception {
        startJobAndWait(infinispanRepositoryJobXml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

}
