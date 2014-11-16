/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.infinispanRepositoryJdbc;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

import javax.batch.runtime.BatchStatus;

public class JdbcInfinispanRepositoryIT extends AbstractIT {
    @Test
    public void partitionWithInfinispanJdbc() throws Exception {
        startJobAndWait(infinispanRepositoryJobXml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
